package com.example.momentra.analytics

/** Stable screen names for Firebase Analytics / BigQuery. */
object AnalyticsScreens {
    const val SPLASH = "screen_splash"
    const val SESSION_RESTORE = "screen_session_restore"
    const val ONBOARDING = "screen_onboarding"
    const val ONBOARDING_SCENE_1 = "screen_onboarding_scene_1"
    const val ONBOARDING_SCENE_2 = "screen_onboarding_scene_2"
    const val ONBOARDING_SCENE_3 = "screen_onboarding_scene_3"
    const val ONBOARDING_PRODUCT_1 = "screen_onboarding_product_1"
    const val ONBOARDING_PRODUCT_2 = "screen_onboarding_product_2"
    const val ONBOARDING_PRODUCT_3 = "screen_onboarding_product_3"
    const val LOGIN = "screen_login"
    const val LOGIN_SIGN_IN = "screen_login_sign_in"
    const val LOGIN_REGISTER = "screen_login_register"
    const val LOGIN_PHONE = "screen_login_phone"
    const val HOME = "screen_home"
    const val PERSONAL_CREATE = "screen_personal_create"
    const val PERSONAL_SETUP_LIFE_OPS = "screen_personal_setup_life_ops"
    const val PERSONAL_SETUP_FUTURE = "screen_personal_setup_future"
    const val PERSONAL_SETUP_LIFESTYLE = "screen_personal_setup_lifestyle"
    const val PERSONAL_SETUP_RELATIONSHIPS = "screen_personal_setup_relationships"
    const val COMPANY_SETUP = "screen_company_setup"
    const val BUSINESS_CREATE = "screen_business_create"
    const val BUSINESS_SETUP_TEAM_OPS = "screen_business_setup_team_ops"
    const val BUSINESS_SETUP_RUNWAY = "screen_business_setup_runway"
    const val BUSINESS_SETUP_OPS = "screen_business_setup_ops"
}

/** Stable widget identifiers — format: screen/widget_action. */
object AnalyticsWidgets {
    const val ONBOARDING_SKIP = "onboarding/btn_skip"
    const val ONBOARDING_STEP_INSIDE = "onboarding/btn_step_inside"
    const val ONBOARDING_NEXT = "onboarding/btn_next"
    const val ONBOARDING_GET_STARTED = "onboarding/btn_get_started"
    const val LOGIN_TAB_SIGN_IN = "login/tab_sign_in"
    const val LOGIN_TAB_REGISTER = "login/tab_register"
    const val LOGIN_TAB_PHONE = "login/tab_phone"
    const val LOGIN_BTN_EMAIL_SUBMIT = "login/btn_email_submit"
    const val LOGIN_BTN_PHONE_SEND = "login/btn_phone_send_sms"
    const val LOGIN_BTN_PHONE_VERIFY = "login/btn_phone_verify"
    const val LOGIN_BTN_PHONE_CHANGE = "login/btn_phone_change_number"
    const val LOGIN_BTN_GOOGLE = "login/btn_google"
    const val SPLASH_COMPLETE = "splash/event_complete"
}
