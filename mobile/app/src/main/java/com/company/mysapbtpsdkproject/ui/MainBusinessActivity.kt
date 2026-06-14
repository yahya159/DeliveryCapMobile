package com.company.mysapbtpsdkproject.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.slf4j.LoggerFactory
import com.sap.cloud.mobile.flows.compose.ui.FlowComposeTheme
import com.company.mysapbtpsdkproject.ui.odata.ODataActivity

class MainBusinessActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlowComposeTheme {
				OfflineInitScreen(
				    navigateToEntityList = 
						::navigateToEntityList,
				    navigateToWelcome = ::navigateToWelcome
				)
            }
        }
    }

    private fun navigateToEntityList() {
        startActivity(Intent(this, ODataActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    private fun navigateToWelcome() {
        startActivity(Intent(this, WelcomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainBusinessActivity::class.java)
    }
}

