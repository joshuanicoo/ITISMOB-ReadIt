package com.mobdeve.s17.group39.itismob_mco

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.mobdeve.s17.group39.itismob_mco.databinding.LoginActivityBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var data: ArrayList<LoginOnboardingModel>
    private lateinit var binding: LoginActivityBinding
    private lateinit var adapter: LoginAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.data = loadData()

        // Snap on scroll thingy
        val snapHelper: SnapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(binding.loginRv)

        this.adapter = LoginAdapter(data)
        this.binding.loginRv.adapter = adapter
        this.binding.loginRv.layoutManager = LinearLayoutManager(this,
            LinearLayoutManager.HORIZONTAL,
            false
        )
    }

    private fun loadData(): ArrayList<LoginOnboardingModel> {
        val tempData = ArrayList<LoginOnboardingModel>()
        tempData.add(LoginOnboardingModel("Your gateway to countless stories.",
                                        "Track the books you love, rate and review every " +
                                                "page-turner, and build your virtual bookshelf for the world to see.",
                                            R.drawable.reading_goose_1))
        return tempData
    }
}