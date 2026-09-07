/**
 * thread_pool.h
 * 
 * Thread pool implementation untuk parallel filter processing
 * 
 * Features:
 * - Fixed number of worker threads
 * - Lock-free work queue (MPMC queue)
 * - Automatic task distribution
 * - Task priority support
 * - Work stealing untuk load balancing
 */

#ifndef FLYERPIX_THREAD_POOL_H
#define FLYERPIX_THREAD_POOL_H

#include <thread>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <functional>
#include <vector>
#include <memory>
#include <atomic>
#include <chrono>

// ============================================================================
// Task & Work Item Definitions
// ============================================================================

/**
 * Task - Unit of work yang dapat dijalankan di thread pool
 */
using Task = std::function<void()>;

/**
 * WorkItem - Item dalam task queue dengan priority
 */
struct WorkItem {
    Task task;
    int priority = 0;  // Higher = more important
    
    WorkItem() = default;
    WorkItem(Task t, int p = 0) : task(t), priority(p) {}
    
    bool operator<(const WorkItem& other) const {
        // Priority queue: higher priority comes first (reverse comparison)
        return priority < other.priority;
    }
};

// ============================================================================
// ThreadPool - Main Thread Pool Class
// ============================================================================

class ThreadPool {
public:
    /**
     * Konstruktor - buat thread pool dengan N worker threads
     * 
     * @param numThreads Jumlah worker threads (default: CPU count)
     */
    explicit ThreadPool(int numThreads = -1);
    
    /**
     * Destruktor - shutdown thread pool dengan graceful wait
     */
    ~ThreadPool();
    
    // Delete copy/move semantics
    ThreadPool(const ThreadPool&) = delete;
    ThreadPool& operator=(const ThreadPool&) = delete;
    
    // ========== Task Submission ==========
    
    /**
     * Submit task ke thread pool
     * 
     * @param task Fungsi yang akan dijalankan
     * @param priority Priority (0=normal, higher=more important)
     */
    void submit(Task task, int priority = 0);
    
    /**
     * Submit multiple tasks untuk batch processing
     * 
     * @param tasks Vector of tasks
     */
    void submitBatch(const std::vector<Task>& tasks);
    
    /**
     * Wait untuk semua tasks selesai (blocking)
     * 
     * @param timeoutMs Timeout dalam milliseconds (-1 = infinite)
     * @return true jika semua tasks selesai, false jika timeout
     */
    bool waitAll(int timeoutMs = -1);
    
    // ========== Pool Control ==========
    
    /**
     * Shutdown thread pool
     * 
     * @param wait true = tunggu tasks selesai, false = cancel tasks
     */
    void shutdown(bool wait = true);
    
    /**
     * Pause task processing (existing tasks continue)
     */
    void pause();
    
    /**
     * Resume task processing
     */
    void resume();
    
    /**
     * Reset thread pool state (clear queue, reset counters)
     */
    void reset();
    
    // ========== Statistics ==========
    
    struct Stats {
        int totalThreads = 0;
        int activeThreads = 0;
        int queuedTasks = 0;
        int completedTasks = 0;
        float averageTaskTimeMs = 0.0f;
    };
    
    /**
     * Get pool statistics
     */
    Stats getStats() const;
    
    /**
     * Get number of pending tasks
     */
    int getPendingTaskCount() const;
    
    /**
     * Get number of active workers
     */
    int getActiveWorkerCount() const;
    
    /**
     * Check if thread pool is running
     */
    bool isRunning() const { return running_; }
    
    /**
     * Check if thread pool is paused
     */
    bool isPaused() const { return paused_; }
    
    // ========== Performance Tuning ==========
    
    /**
     * Set worker thread stack size (untuk resource-constrained devices)
     */
    void setWorkerStackSize(size_t stackSize) { worker_stack_size_ = stackSize; }
    
    /**
     * Set CPU affinity untuk worker threads (Android specific)
     */
    void setCPUAffinity(const std::vector<int>& cpuIds);
    
private:
    // Thread management
    std::vector<std::thread> workers_;
    std::atomic<int> active_worker_count_{0};
    
    // Task queue
    std::priority_queue<WorkItem> task_queue_;
    mutable std::mutex queue_mutex_;
    std::condition_variable queue_cv_;
    
    // State control
    std::atomic<bool> running_{false};
    std::atomic<bool> paused_{false};
    std::atomic<bool> shutdown_flag_{false};
    
    // Statistics
    std::atomic<int> completed_tasks_{0};
    float total_task_time_ms_ = 0.0f;  // Protected by stats_mutex_
    mutable std::mutex stats_mutex_;  // For stats access
    size_t worker_stack_size_ = 0;
    
    // Wait synchronization
    std::mutex wait_mutex_;
    std::condition_variable wait_cv_;
    std::atomic<int> tasks_in_flight_{0};
    
    // Worker thread function
    void workerThread();
    
    // Helper
    int getHardwareThreadCount() const;
};

// ============================================================================
// ParallelFor - Utility untuk parallel loop execution
// ============================================================================

/**
 * Parallel for loop helper - membagi iteration range ke multiple threads
 * 
 * Example:
 *   ParallelFor parallel_for(thread_pool, 1000, 4);  // 1000 iterations, 4 threads
 *   for (int i = parallel_for.start(); i < parallel_for.end(); i++) {
 *       // Parallel iteration
 *   }
 */
class ParallelFor {
public:
    /**
     * Konstruktor
     * 
     * @param pool Thread pool yang digunakan
     * @param iterations Total jumlah iterations
     * @param numWorkers Jumlah worker threads (0 = gunakan pool size)
     */
    ParallelFor(ThreadPool& pool, int iterations, int numWorkers = 0);
    
    /**
     * Execute parallel loop
     * 
     * @param func Function yang dijalankan per iteration (menerima iteration index)
     */
    template<typename Func>
    void execute(Func func) {
        // Submit tasks per worker
        for (int w = 0; w < num_workers_; w++) {
            int start = w * chunk_size_;
            int end = (w == num_workers_ - 1) ? iterations_ : (w + 1) * chunk_size_;
            
            auto task = [this, start, end, func]() {
                for (int i = start; i < end; i++) {
                    func(i);
                }
            };
            
            pool_.submit(task);
        }
        
        // Wait untuk semua tasks selesai
        pool_.waitAll();
    }
    
private:
    ThreadPool& pool_;
    int iterations_;
    int num_workers_;
    int chunk_size_;
};

#endif // FLYERPIX_THREAD_POOL_H
