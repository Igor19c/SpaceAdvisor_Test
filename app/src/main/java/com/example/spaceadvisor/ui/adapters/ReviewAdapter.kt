package com.example.spaceadvisor.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.spaceadvisor.R
import com.example.spaceadvisor.databinding.ItemReviewBinding
import com.example.spaceadvisor.domain.models.Review
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(
    private var reviews: List<Review> = listOf()
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    fun updateData(newReviews: List<Review>) {
        this.reviews = newReviews
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        holder.bind(review)
    }

    override fun getItemCount(): Int = reviews.size

    class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(review: Review) {

            Glide.with(binding.userPicItemReview.context)
                .load(review.userProfileImage)
                .placeholder(R.drawable.pic_profile)
                .circleCrop()
                .into(binding.userPicItemReview)

            binding.reviewerNameItemReview.text = review.username
            val reviewContent: String
            if (review.comment.isNotEmpty())
                reviewContent = "\"${review.comment}\""
            else
                reviewContent = review.comment

            binding.reviewContentItemReview.text = reviewContent

            binding.destinationTitleItemReview.text = review.destinationTitle

            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            binding.reviewDateItemReview.text =
                if (review.createdAt != null) sdf.format(Date(review.createdAt)) else ""

            val stars =
                listOf(
                    binding.star1,
                    binding.star2,
                    binding.star3,
                    binding.star4,
                    binding.star5
                )
            for (i in stars.indices) {
                if (i < review.rating) {
                    stars[i].setImageResource(R.drawable.ic_rating_star_filled)
                } else {
                    stars[i].setImageResource(R.drawable.ic_rating_star_outlined)
                }
            }
        }
    }

}