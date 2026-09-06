package com.flyerpix.editor.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.chip.ChipGroup
import com.flyerpix.editor.R
import com.flyerpix.editor.canvas.model.StickerCategory
import com.flyerpix.editor.canvas.model.StickerItem
import com.flyerpix.editor.databinding.FragmentTabStickerBinding
import com.flyerpix.editor.ui.adapter.StickerPickerAdapter

/**
 * Tab Sticker Fragment.
 *
 * Menampilkan picker stiker/emoji dalam format grid dengan filter kategori
 * menggunakan horizontal chip group. Saat sebuah stiker/emoji dipilih,
 * [TabStickerListener.onStickerSelected] dipanggil.
 */
class TabSticker : Fragment() {

    private var _binding: FragmentTabStickerBinding? = null
    private val binding get() = _binding!!

    lateinit var tabStickerListener: TabStickerListener

    private lateinit var adapter: StickerPickerAdapter
    private var currentCategory: StickerCategory? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is TabStickerListener) {
            tabStickerListener = context
        } else {
            throw RuntimeException("$context must implement TabStickerListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTabStickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryChips()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = StickerPickerAdapter(getAllStickers()) { stickerItem ->
            tabStickerListener.onStickerSelected(stickerItem)
        }
        binding.rvStickerGrid.layoutManager = GridLayoutManager(requireContext(), 6)
        binding.rvStickerGrid.adapter = adapter
    }

    private fun setupCategoryChips() {
        binding.chipGroupCategory.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds[0]
            val filtered = when (checkedId) {
                R.id.chipSmileys  -> getStickersForCategory(StickerCategory.SMILEYS)
                R.id.chipAnimals  -> getStickersForCategory(StickerCategory.ANIMALS)
                R.id.chipFood     -> getStickersForCategory(StickerCategory.FOOD)
                R.id.chipActivities -> getStickersForCategory(StickerCategory.ACTIVITIES)
                R.id.chipTravel   -> getStickersForCategory(StickerCategory.TRAVEL)
                R.id.chipObjects  -> getStickersForCategory(StickerCategory.OBJECTS)
                R.id.chipSymbols  -> getStickersForCategory(StickerCategory.SYMBOLS)
                else              -> getAllStickers()
            }
            adapter.updateItems(filtered)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sticker Data (built-in emoji)
    // ─────────────────────────────────────────────────────────────────────────

    private fun getStickersForCategory(category: StickerCategory): List<StickerItem> {
        return STICKER_DATA.filter { it.category == category }
    }

    private fun getAllStickers(): List<StickerItem> = STICKER_DATA

    // ─────────────────────────────────────────────────────────────────────────
    // Listener Interface
    // ─────────────────────────────────────────────────────────────────────────

    interface TabStickerListener {
        fun onStickerSelected(stickerItem: StickerItem)
    }

    companion object {
        /**
         * Data stiker/emoji bawaan per kategori.
         */
        private val STICKER_DATA: List<StickerItem> = listOf(
            // ── Smileys & People ──────────────────────────────────────────
            StickerItem("😀", "Grinning", StickerCategory.SMILEYS),
            StickerItem("😃", "Grin", StickerCategory.SMILEYS),
            StickerItem("😄", "Laugh", StickerCategory.SMILEYS),
            StickerItem("😁", "Beam", StickerCategory.SMILEYS),
            StickerItem("😆", "Tight Laugh", StickerCategory.SMILEYS),
            StickerItem("😅", "Sweat Smile", StickerCategory.SMILEYS),
            StickerItem("🤣", "ROFL", StickerCategory.SMILEYS),
            StickerItem("😂", "Joy Tears", StickerCategory.SMILEYS),
            StickerItem("🙂", "Slight Smile", StickerCategory.SMILEYS),
            StickerItem("🙃", "Upside Down", StickerCategory.SMILEYS),
            StickerItem("😉", "Wink", StickerCategory.SMILEYS),
            StickerItem("😊", "Blush", StickerCategory.SMILEYS),
            StickerItem("😇", "Halo", StickerCategory.SMILEYS),
            StickerItem("🥰", "Hearts Face", StickerCategory.SMILEYS),
            StickerItem("😍", "Heart Eyes", StickerCategory.SMILEYS),
            StickerItem("🤩", "Star Struck", StickerCategory.SMILEYS),
            StickerItem("😘", "Kiss", StickerCategory.SMILEYS),
            StickerItem("😎", "Cool", StickerCategory.SMILEYS),
            StickerItem("🤓", "Nerd", StickerCategory.SMILEYS),
            StickerItem("🧐", "Monocle", StickerCategory.SMILEYS),
            StickerItem("🤔", "Thinking", StickerCategory.SMILEYS),
            StickerItem("🤗", "Hug", StickerCategory.SMILEYS),
            StickerItem("😱", "Scream", StickerCategory.SMILEYS),
            StickerItem("🥳", "Party", StickerCategory.SMILEYS),
            StickerItem("😤", "Huff", StickerCategory.SMILEYS),

            // ── Animals & Nature ──────────────────────────────────────────
            StickerItem("🐶", "Dog", StickerCategory.ANIMALS),
            StickerItem("🐱", "Cat", StickerCategory.ANIMALS),
            StickerItem("🐭", "Mouse", StickerCategory.ANIMALS),
            StickerItem("🐹", "Hamster", StickerCategory.ANIMALS),
            StickerItem("🐰", "Rabbit", StickerCategory.ANIMALS),
            StickerItem("🦊", "Fox", StickerCategory.ANIMALS),
            StickerItem("🐻", "Bear", StickerCategory.ANIMALS),
            StickerItem("🐼", "Panda", StickerCategory.ANIMALS),
            StickerItem("🐨", "Koala", StickerCategory.ANIMALS),
            StickerItem("🐯", "Tiger", StickerCategory.ANIMALS),
            StickerItem("🦁", "Lion", StickerCategory.ANIMALS),
            StickerItem("🐮", "Cow", StickerCategory.ANIMALS),
            StickerItem("🐷", "Pig", StickerCategory.ANIMALS),
            StickerItem("🐸", "Frog", StickerCategory.ANIMALS),
            StickerItem("🐵", "Monkey", StickerCategory.ANIMALS),
            StickerItem("🐔", "Chicken", StickerCategory.ANIMALS),
            StickerItem("🐧", "Penguin", StickerCategory.ANIMALS),
            StickerItem("🐦", "Bird", StickerCategory.ANIMALS),
            StickerItem("🦅", "Eagle", StickerCategory.ANIMALS),
            StickerItem("🦋", "Butterfly", StickerCategory.ANIMALS),
            StickerItem("🌸", "Cherry Blossom", StickerCategory.ANIMALS),
            StickerItem("🌺", "Hibiscus", StickerCategory.ANIMALS),
            StickerItem("🌻", "Sunflower", StickerCategory.ANIMALS),
            StickerItem("🌲", "Tree", StickerCategory.ANIMALS),
            StickerItem("🍀", "Clover", StickerCategory.ANIMALS),

            // ── Food & Drink ─────────────────────────────────────────────
            StickerItem("🍔", "Burger", StickerCategory.FOOD),
            StickerItem("🍕", "Pizza", StickerCategory.FOOD),
            StickerItem("🍟", "Fries", StickerCategory.FOOD),
            StickerItem("🌭", "Hot Dog", StickerCategory.FOOD),
            StickerItem("🍿", "Popcorn", StickerCategory.FOOD),
            StickerItem("🍣", "Sushi", StickerCategory.FOOD),
            StickerItem("🍰", "Cake", StickerCategory.FOOD),
            StickerItem("🍩", "Donut", StickerCategory.FOOD),
            StickerItem("🍪", "Cookie", StickerCategory.FOOD),
            StickerItem("🍫", "Chocolate", StickerCategory.FOOD),
            StickerItem("🍬", "Candy", StickerCategory.FOOD),
            StickerItem("☕", "Coffee", StickerCategory.FOOD),
            StickerItem("🍵", "Tea", StickerCategory.FOOD),
            StickerItem("🥤", "Soda", StickerCategory.FOOD),
            StickerItem("🧃", "Juice Box", StickerCategory.FOOD),
            StickerItem("🍷", "Wine", StickerCategory.FOOD),
            StickerItem("🍺", "Beer", StickerCategory.FOOD),
            StickerItem("🥂", "Cheers", StickerCategory.FOOD),
            StickerItem("🍦", "Ice Cream", StickerCategory.FOOD),
            StickerItem("🎂", "Birthday Cake", StickerCategory.FOOD),
            StickerItem("🍎", "Apple", StickerCategory.FOOD),
            StickerItem("🍊", "Orange", StickerCategory.FOOD),
            StickerItem("🍇", "Grapes", StickerCategory.FOOD),
            StickerItem("🍓", "Strawberry", StickerCategory.FOOD),
            StickerItem("🥑", "Avocado", StickerCategory.FOOD),

            // ── Activities ────────────────────────────────────────────────
            StickerItem("⚽", "Soccer", StickerCategory.ACTIVITIES),
            StickerItem("🏀", "Basketball", StickerCategory.ACTIVITIES),
            StickerItem("🏈", "Football", StickerCategory.ACTIVITIES),
            StickerItem("⚾", "Baseball", StickerCategory.ACTIVITIES),
            StickerItem("🎾", "Tennis", StickerCategory.ACTIVITIES),
            StickerItem("🎮", "Game", StickerCategory.ACTIVITIES),
            StickerItem("🎯", "Dart", StickerCategory.ACTIVITIES),
            StickerItem("🏆", "Trophy", StickerCategory.ACTIVITIES),
            StickerItem("🎪", "Circus", StickerCategory.ACTIVITIES),
            StickerItem("🎨", "Palette", StickerCategory.ACTIVITIES),
            StickerItem("🎬", "Clapperboard", StickerCategory.ACTIVITIES),
            StickerItem("🎤", "Mic", StickerCategory.ACTIVITIES),
            StickerItem("🎧", "Headphones", StickerCategory.ACTIVITIES),
            StickerItem("🎸", "Guitar", StickerCategory.ACTIVITIES),
            StickerItem("🎺", "Trumpet", StickerCategory.ACTIVITIES),
            StickerItem("🥁", "Drum", StickerCategory.ACTIVITIES),
            StickerItem("🎲", "Dice", StickerCategory.ACTIVITIES),
            StickerItem("♟️", "Chess", StickerCategory.ACTIVITIES),
            StickerItem("🎳", "Bowling", StickerCategory.ACTIVITIES),
            StickerItem("🪁", "Kite", StickerCategory.ACTIVITIES),
            StickerItem("🎪", "Tent", StickerCategory.ACTIVITIES),
            StickerItem("🎭", "Masks", StickerCategory.ACTIVITIES),
            StickerItem("🎬", "Film", StickerCategory.ACTIVITIES),
            StickerItem("🎡", "Ferris Wheel", StickerCategory.ACTIVITIES),
            StickerItem("🎢", "Roller Coaster", StickerCategory.ACTIVITIES),

            // ── Travel & Places ───────────────────────────────────────────
            StickerItem("🚗", "Car", StickerCategory.TRAVEL),
            StickerItem("🚕", "Taxi", StickerCategory.TRAVEL),
            StickerItem("🚌", "Bus", StickerCategory.TRAVEL),
            StickerItem("🚀", "Rocket", StickerCategory.TRAVEL),
            StickerItem("✈️", "Airplane", StickerCategory.TRAVEL),
            StickerItem("🚢", "Ship", StickerCategory.TRAVEL),
            StickerItem("🏠", "House", StickerCategory.TRAVEL),
            StickerItem("🏢", "Building", StickerCategory.TRAVEL),
            StickerItem("🏰", "Castle", StickerCategory.TRAVEL),
            StickerItem("🗼", "Tower", StickerCategory.TRAVEL),
            StickerItem("🗽", "Statue", StickerCategory.TRAVEL),
            StickerItem("🌍", "Globe", StickerCategory.TRAVEL),
            StickerItem("🗺️", "Map", StickerCategory.TRAVEL),
            StickerItem("⛰️", "Mountain", StickerCategory.TRAVEL),
            StickerItem("🏖️", "Beach", StickerCategory.TRAVEL),
            StickerItem("🌅", "Sunrise", StickerCategory.TRAVEL),
            StickerItem("🌇", "Sunset", StickerCategory.TRAVEL),
            StickerItem("🌌", "Galaxy", StickerCategory.TRAVEL),
            StickerItem("🌈", "Rainbow", StickerCategory.TRAVEL),
            StickerItem("❄️", "Snowflake", StickerCategory.TRAVEL),
            StickerItem("🔥", "Fire", StickerCategory.TRAVEL),
            StickerItem("💧", "Water Drop", StickerCategory.TRAVEL),
            StickerItem("🌊", "Wave", StickerCategory.TRAVEL),
            StickerItem("⚡", "Lightning", StickerCategory.TRAVEL),
            StickerItem("🌙", "Moon", StickerCategory.TRAVEL),

            // ── Objects ──────────────────────────────────────────────────
            StickerItem("💡", "Light Bulb", StickerCategory.OBJECTS),
            StickerItem("📱", "Phone", StickerCategory.OBJECTS),
            StickerItem("💻", "Laptop", StickerCategory.OBJECTS),
            StickerItem("⌨️", "Keyboard", StickerCategory.OBJECTS),
            StickerItem("🖥️", "Monitor", StickerCategory.OBJECTS),
            StickerItem("📷", "Camera", StickerCategory.OBJECTS),
            StickerItem("📺", "TV", StickerCategory.OBJECTS),
            StickerItem("🔦", "Flashlight", StickerCategory.OBJECTS),
            StickerItem("🔑", "Key", StickerCategory.OBJECTS),
            StickerItem("🔒", "Lock", StickerCategory.OBJECTS),
            StickerItem("💎", "Gem", StickerCategory.OBJECTS),
            StickerItem("🧲", "Magnet", StickerCategory.OBJECTS),
            StickerItem("🎁", "Gift", StickerCategory.OBJECTS),
            StickerItem("🎈", "Balloon", StickerCategory.OBJECTS),
            StickerItem("🧸", "Teddy Bear", StickerCategory.OBJECTS),
            StickerItem("🎉", "Confetti", StickerCategory.OBJECTS),
            StickerItem("🔔", "Bell", StickerCategory.OBJECTS),
            StickerItem("📦", "Package", StickerCategory.OBJECTS),
            StickerItem("✏️", "Pencil", StickerCategory.OBJECTS),
            StickerItem("📝", "Memo", StickerCategory.OBJECTS),
            StickerItem("📌", "Pin", StickerCategory.OBJECTS),
            StickerItem("📎", "Paperclip", StickerCategory.OBJECTS),
            StickerItem("🔧", "Wrench", StickerCategory.OBJECTS),
            StickerItem("🔨", "Hammer", StickerCategory.OBJECTS),
            StickerItem("⚙️", "Gear", StickerCategory.OBJECTS),

            // ── Symbols ──────────────────────────────────────────────────
            StickerItem("❤️", "Heart", StickerCategory.SYMBOLS),
            StickerItem("🧡", "Orange Heart", StickerCategory.SYMBOLS),
            StickerItem("💛", "Yellow Heart", StickerCategory.SYMBOLS),
            StickerItem("💚", "Green Heart", StickerCategory.SYMBOLS),
            StickerItem("💙", "Blue Heart", StickerCategory.SYMBOLS),
            StickerItem("💜", "Purple Heart", StickerCategory.SYMBOLS),
            StickerItem("🖤", "Black Heart", StickerCategory.SYMBOLS),
            StickerItem("🤍", "White Heart", StickerCategory.SYMBOLS),
            StickerItem("💔", "Broken Heart", StickerCategory.SYMBOLS),
            StickerItem("💕", "Two Hearts", StickerCategory.SYMBOLS),
            StickerItem("💖", "Sparkling Heart", StickerCategory.SYMBOLS),
            StickerItem("✨", "Sparkles", StickerCategory.SYMBOLS),
            StickerItem("⭐", "Star", StickerCategory.SYMBOLS),
            StickerItem("🌟", "Glowing Star", StickerCategory.SYMBOLS),
            StickerItem("💫", "Dizzy", StickerCategory.SYMBOLS),
            StickerItem("✅", "Check", StickerCategory.SYMBOLS),
            StickerItem("❌", "Cross", StickerCategory.SYMBOLS),
            StickerItem("❗", "Exclamation", StickerCategory.SYMBOLS),
            StickerItem("❓", "Question", StickerCategory.SYMBOLS),
            StickerItem("💤", "Zzz", StickerCategory.SYMBOLS),
            StickerItem("💢", "Anger", StickerCategory.SYMBOLS),
            StickerItem("💥", "Boom", StickerCategory.SYMBOLS),
            StickerItem("🔴", "Red Circle", StickerCategory.SYMBOLS),
            StickerItem("🟢", "Green Circle", StickerCategory.SYMBOLS),
            StickerItem("🔵", "Blue Circle", StickerCategory.SYMBOLS)
        )
    }
}
