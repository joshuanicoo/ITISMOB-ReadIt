package com.mobdeve.s17.group39.itismob_mco.features.authentication.login

import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.databinding.LoginItemLayoutBinding
import com.mobdeve.s17.group39.itismob_mco.features.authentication.login.LoginOnboardingModel

class LoginViewHolder(private val viewBinding: LoginItemLayoutBinding): RecyclerView.ViewHolder(viewBinding.root) {

    fun bindData(data: LoginOnboardingModel) {
        this.viewBinding.loginImageIv.setImageResource(data.imageId)


        this.viewBinding.loginHeaderTv.text = data.header
        this.viewBinding.loginBodyTv.text = data.body
    }

}