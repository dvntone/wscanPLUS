package com.wscanplus.app.cti

import com.wscanplus.app.BuildConfig

interface CrowdSecCtiKeyProvider {
    fun getApiKey(): String
}

class BuildConfigCrowdSecCtiKeyProvider : CrowdSecCtiKeyProvider {
    override fun getApiKey(): String = BuildConfig.CROWDSEC_CTI_API_KEY
}
