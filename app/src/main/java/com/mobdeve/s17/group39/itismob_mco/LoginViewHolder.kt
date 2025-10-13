package com.mobdeve.s17.group39.itismob_mco

import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.LoginItemLayoutBinding

class LoginViewHolder(private val viewBinding: LoginItemLayoutBinding): RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: LoginOnboardingModel) {
        this.viewBinding.loginImageIv.setImageResource(data.imageId)


        this.viewBinding.loginHeaderTv.text = data.header
        this.viewBinding.loginBodyTv.text = data.body
    }

}