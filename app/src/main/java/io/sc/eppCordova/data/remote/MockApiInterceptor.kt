package io.sc.eppCordova.data.remote

import android.content.Context
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

class MockApiInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val path = chain.request().url.encodedPath

        val json = when {
            path.contains("sendOtp") ->
                """{"success":true,"message":"OTP sent successfully"}"""

            path.contains("verifyOtp") ->
                """{"success":true,"token":"MOCK_TOKEN_XYZ","message":"Verified"}"""

            path.contains("divisions") ->
                """["पुणे विभाग","मुंबई विभाग","नाशिक विभाग","औरंगाबाद विभाग","अमरावती विभाग","नागपूर विभाग"]"""

            path.contains("districts") ->
                """["पुणे","सातारा","सोलापूर","कोल्हापूर","सांगली"]"""

            path.contains("talukas") ->
                """["हवेली","मुळशी","खेड","जुन्नर","आंबेगाव","भोर"]"""

            path.contains("villages") ->
                """["उरुळी कांचन","लोहगाव","मांजरी","वाघोली","केसनंद","फुरसुंगी"]"""

            path.contains("parcels") ->
                // Returns JSON list compatible with LandRecord entity
                """[
                  {"gutNo":"87","khataNo":"142","ownerName":"राजेश विठ्ठल पाटील","areaHectares":1.20,"villageId":1},
                  {"gutNo":"88","khataNo":"143","ownerName":"सुनिता पाटील","areaHectares":0.80,"villageId":1}
                ]"""

            path.contains("upload") ->
                """{"success":true,"url":"https://mock-storage.gov/evidence/mock.jpg","message":"Uploaded successfully"}"""

            path.contains("claims") || path.contains("surveys") ->
                """{"claimId":"REP-12345","farmerId":"Mock","workflowStage":"AI Verification","createdAt":"2026-05-15"}"""

            path.contains("reports") ->
                """{"reportId":"REP-12345","workflowStage":"AI Analysis Passed","confidenceScore":92.5,"geoVerified":true,"severityLevel":"High","aiRemarks":"Visible damage identified by AI matches selected disaster.","officerRemarks":null,"assignedOfficer":"Verification Desk A","createdAt":"2026-05-15"}"""

            path.contains("submitSurvey") ->
                """{"success":true,"message":"Survey submitted successfully"}"""

            else ->
                """{"success":false,"message":"endpoint not mocked"}"""
        }

        return Response.Builder()
            .code(200)
            .body(json.toResponseBody("application/json".toMediaType()))
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .message("OK")
            .build()
    }
}
