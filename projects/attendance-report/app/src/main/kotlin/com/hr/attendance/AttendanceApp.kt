package com.hr.attendance

import com.hr.attendance.data.api.AttendanceApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object AttendanceApp {
    private const val BASE_URL = "https://hr.example.com/"

    val api: AttendanceApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AttendanceApiService::class.java)
    }
}
