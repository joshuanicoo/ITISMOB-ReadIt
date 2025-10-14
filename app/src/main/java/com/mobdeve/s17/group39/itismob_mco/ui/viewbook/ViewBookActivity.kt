package com.mobdeve.s17.group39.itismob_mco.ui.viewbook

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.mobdeve.s17.group39.itismob_mco.databinding.ViewBookActivityBinding

class ViewBookActivity : AppCompatActivity() {
    companion object {
        // Constants for data passing
        const val TITLE_KEY = "TITLE_KEY"
        const val AUTHOR_KEY = "AUTHOR_KEY"
        const val DESCRIPTION_KEY = "DESCRIPTION_KEY"
        const val AVG_RATING_COUNT_KEY = "AVG_RATING_COUNT_KEY"
        const val RATING_COUNT_KEY = "RATING_COUNT_KEY"
        const val POSITION_KEY = "POSITION_KEY"
    }

    private lateinit var viewBinding: ViewBookActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        this.viewBinding = ViewBookActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        val titleString = intent.getStringExtra(TITLE_KEY).toString()
        val authorString = intent.getStringExtra(AUTHOR_KEY).toString()
        val descriptionString = intent.getStringExtra(DESCRIPTION_KEY).toString()
        val avgRatingDouble = intent.getDoubleExtra(AVG_RATING_COUNT_KEY, 0.0)
        val ratingCountInt = intent.getIntExtra(RATING_COUNT_KEY, 0)
        val position = intent.getIntExtra(POSITION_KEY, -1)

        viewBinding.titleTv.text = titleString
        viewBinding.authorTv.text = authorString
        viewBinding.descriptionTv.text = descriptionString
        viewBinding.avgRatingRb.rating = avgRatingDouble.toFloat()
        viewBinding.numberOfRatingsTv.text = ratingCountInt.toString()

    }

}