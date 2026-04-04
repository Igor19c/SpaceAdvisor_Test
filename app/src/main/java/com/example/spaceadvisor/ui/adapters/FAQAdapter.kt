package com.example.spaceadvisor.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.databinding.FragmentFaqBinding
import com.example.spaceadvisor.databinding.ItemBadgeBinding
import com.example.spaceadvisor.databinding.ItemFaqBinding
import com.example.spaceadvisor.domain.models.Badge
import com.example.spaceadvisor.domain.models.FAQ

class FAQAdapter(
    private var faqs: List<FAQ> = listOf(),
) : RecyclerView.Adapter<FAQAdapter.FaqViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FaqViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val faq = faqs[position]

        holder.binding.faqQuestion.text = faq.question
        holder.binding.faqAnswer.text = faq.answer

        holder.binding.faqCard.setOnClickListener {
            val isVisible = holder.binding.faqAnswer.visibility == View.VISIBLE
            if (isVisible) {
                holder.binding.faqAnswer.visibility = View.GONE
                holder.binding.faqArrow.animate().rotation(90f).setDuration(200).start()
            } else {
                holder.binding.faqAnswer.visibility = View.VISIBLE
                holder.binding.faqArrow.animate().rotation(-90f).setDuration(200).start()
            }
        }


    }


    override fun getItemCount(): Int = faqs.size

    class FaqViewHolder(val binding: ItemFaqBinding) : RecyclerView.ViewHolder(binding.root)
}