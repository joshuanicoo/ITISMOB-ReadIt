package com.mobdeve.s17.group39.itismob_mco.login

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.group39.itismob_mco.login.LoginOnboardingModel
import com.mobdeve.s17.group39.itismob_mco.login.LoginViewHolder
import com.mobdeve.s17.group39.itismob_mco.databinding.LoginItemLayoutBinding

class LoginAdapter(private val data: ArrayList<LoginOnboardingModel>): RecyclerView.Adapter<LoginViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoginViewHolder {
        val itemViewBinding: LoginItemLayoutBinding = LoginItemLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        val myViewHolder = LoginViewHolder(itemViewBinding)

        return myViewHolder
    }

    override fun onBindViewHolder(holder: LoginViewHolder, position: Int) {
        holder.bindData(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}