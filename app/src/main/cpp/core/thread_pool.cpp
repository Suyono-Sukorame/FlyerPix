/**
 * thread_pool.cpp
 * 
 * Thread pool implementation untuk parallel filter processing
 */

#include "thread_pool.h"
#include <algorithm>
#include <android/log.h>
#include <unistd.h>
#include <sys/sysconf.h>

#define LOG_TAG "ThreadPool"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// ThreadPool Implementation
// ============================================================================

ThreadPool::ThreadPool(int numThreads) {
    // Determine number of threads
    if (numThreads <= 0) {
        numThreads = getHardwareThreadCount();
    }
    
    if (numThreads < 1) {
        numThreads = 1;  // Fallback
    }
    
    running_ = true;
    
    // Create worker threads
    LOGD("Creating thread pool with %d workers", numThreads);
    
    for (int i = 0; i < numThreads; i++) {
        workers_.emplace_back(&ThreadPool::workerThread, this);
    }
    
    LOGD("Thread pool created successfully");
}

ThreadPool::~ThreadPool() {
    shutdown(true);
}

void ThreadPool::submit(Task task, int priority) {
    if (!running_) {
        LOGE("Cannot submit task: thread pool not running");
        return;
    }
    
    {
        std::lock_guard<std::mutex> lock(queue_mutex_);
        task_queue_.emplace(task, priority);
        tasks_in_flight_++;
    }
    
    queue_cv_.notify_one();
}

void ThreadPool::submitBatch(const std::vector<Task>& tasks) {
    if (!running_) {
        LOGE("Cannot submit batch: thread pool not running");
        return;
    }
    
    {
        std::lock_guard<std::mutex> lock(queue_mutex_);
        for (const auto& task : tasks) {
            task_queue_.emplace(task, 0);
            tasks_in_flight_++;
        }
    }
    
    // Notify all workers
    for (size_t i = 0; i < workers_.size(); i++) {
        queue_cv_.notify_one();
    }
}

bool ThreadPool::waitAll(int timeoutMs) {
    std::unique_lock<std::mutex> lock(wait_mutex_);
    
    auto predicate = [this]() {
        return tasks_in_flight_ == 0;
    };
    
    if (timeoutMs < 0) {
        // Infinite wait
        wait_cv_.wait(lock, predicate);
        return true;
    } else {
        // Wait with timeout
        auto duration = std::chrono::milliseconds(timeoutMs);
        return wait_cv_.wait_for(lock, duration, predicate);
    }
}

void ThreadPool::shutdown(bool wait) {
    if (!running_) {
        return;
    }
    
    LOGD("Shutting down thread pool (wait=%d)", wait);
    
    if (wait) {
        // Wait untuk existing tasks selesai
        waitAll();
    }
    
    // Signal shutdown
    {
        std::lock_guard<std::mutex> lock(queue_mutex_);
        shutdown_flag_ = true;
    }
    
    queue_cv_.notify_all();
    
    // Wait untuk worker threads selesai
    for (auto& worker : workers_) {
        if (worker.joinable()) {
            worker.join();
        }
    }
    
    running_ = false;
    LOGD("Thread pool shutdown complete");
}

void ThreadPool::pause() {
    paused_ = true;
    LOGD("Thread pool paused");
}

void ThreadPool::resume() {
    paused_ = false;
    queue_cv_.notify_all();
    LOGD("Thread pool resumed");
}

void ThreadPool::reset() {
    std::lock_guard<std::mutex> lock(queue_mutex_);
    
    // Clear task queue
    while (!task_queue_.empty()) {
        task_queue_.pop();
    }
    
    // Reset counters
    tasks_in_flight_ = 0;
    completed_tasks_ = 0;
    total_task_time_ms_ = 0.0f;
    
    LOGD("Thread pool reset");
}

ThreadPool::Stats ThreadPool::getStats() const {
    Stats stats;
    stats.totalThreads = workers_.size();
    stats.activeThreads = active_worker_count_;
    stats.queuedTasks = tasks_in_flight_;
    stats.completedTasks = completed_tasks_;
    
    {
        std::lock_guard<std::mutex> lock(stats_mutex_);
        if (completed_tasks_ > 0) {
            stats.averageTaskTimeMs = total_task_time_ms_ / completed_tasks_;
        }
    }
    
    return stats;
}

int ThreadPool::getPendingTaskCount() const {
    return tasks_in_flight_;
}

int ThreadPool::getActiveWorkerCount() const {
    return active_worker_count_;
}

void ThreadPool::setCPUAffinity(const std::vector<int>& cpuIds) {
    // Android specific: tidak fully supported dalam thread pool
    // Ini adalah placeholder untuk future optimization
    LOGD("CPU affinity requested for %zu CPUs (not fully implemented)", cpuIds.size());
}

void ThreadPool::workerThread() {
    LOGD("Worker thread started");
    
    while (running_) {
        WorkItem work_item;
        
        {
            std::unique_lock<std::mutex> lock(queue_mutex_);
            
            // Wait sampai ada task atau shutdown flag
            queue_cv_.wait(lock, [this]() {
                return !task_queue_.empty() || shutdown_flag_;
            });
            
            if (shutdown_flag_ && task_queue_.empty()) {
                // Shutdown requested dan no more tasks
                break;
            }
            
            if (task_queue_.empty()) {
                continue;  // Spurious wakeup
            }
            
            work_item = task_queue_.top();
            task_queue_.pop();
        }
        
        // Execute task outside lock
        if (work_item.task) {
            // Skip task jika paused
            if (paused_) {
                // Put task back ke queue
                submit(work_item.task, work_item.priority);
                std::this_thread::yield();
                continue;
            }
            
            active_worker_count_++;
            
            auto start_time = std::chrono::high_resolution_clock::now();
            
            try {
                work_item.task();
            } catch (const std::exception& e) {
                LOGE("Task execution failed: %s", e.what());
            }
            
            auto end_time = std::chrono::high_resolution_clock::now();
            auto duration = std::chrono::duration_cast<std::chrono::microseconds>(
                end_time - start_time).count();
            
            float task_time_ms = duration / 1000.0f;
            
            {
                std::lock_guard<std::mutex> lock(stats_mutex_);
                total_task_time_ms_ += task_time_ms;
            }
            
            completed_tasks_++;
            
            active_worker_count_--;
        }
        
        // Decrement tasks in flight dan notify waiters
        {
            std::lock_guard<std::mutex> lock(wait_mutex_);
            tasks_in_flight_--;
        }
        
        wait_cv_.notify_all();
    }
    
    LOGD("Worker thread exited");
}

int ThreadPool::getHardwareThreadCount() const {
    // Get number of available CPUs
    long nprocs = sysconf(_SC_NPROCESSORS_ONLN);
    if (nprocs <= 0) {
        nprocs = 1;
    }
    
    // Typically use (nprocs - 1) untuk leave one CPU free
    // Tapi untuk mobile apps, gunakan semua CPUs
    return static_cast<int>(nprocs);
}

// ============================================================================
// ParallelFor Implementation
// ============================================================================

ParallelFor::ParallelFor(ThreadPool& pool, int iterations, int numWorkers)
    : pool_(pool), iterations_(iterations) {
    
    if (numWorkers <= 0) {
        numWorkers = pool.getStats().totalThreads;
    }
    
    num_workers_ = numWorkers;
    chunk_size_ = (iterations_ + num_workers_ - 1) / num_workers_;
    
    if (chunk_size_ < 1) {
        chunk_size_ = 1;
    }
}
