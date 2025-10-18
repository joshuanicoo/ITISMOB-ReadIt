package com.mobdeve.s17.group39.itismob_mco.ui.profile


import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s17.group39.itismob_mco.databinding.ProfileActivityLayoutBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ProfileActivityLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileActivityLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.updateProfileBtn.setOnClickListener {
            Toast.makeText(this, "Updated successfully", Toast.LENGTH_SHORT).show()
        }
    }

}