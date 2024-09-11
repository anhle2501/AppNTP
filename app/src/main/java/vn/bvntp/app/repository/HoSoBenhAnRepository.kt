package vn.bvntp.app.repository

import android.content.Context
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import vn.bvntp.app.api.HoSoBenhAnService
import vn.bvntp.app.helper.retrieveAccessDataDecryptedData
import vn.bvntp.app.model.LichSuDieuTriResponse
import vn.bvntp.app.model.RequestLichSuDieuTri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.concurrent.TimeUnit


class HoSoBenhAnRepository(val hoSoBenhAnService: HoSoBenhAnService) {

    suspend fun getHoSoBenhAn(
        context: Context,
        maVaoVien: String,
        listId: ArrayList<Int>,
        onResult: (Result<File>) -> Unit
    ) = withContext(Dispatchers.IO) {

        val accessToken = retrieveAccessDataDecryptedData(context, "access_token")
        val file = File(context.cacheDir, "tempFileHsba.pdf")
        if (file.isFile) file.delete()

        val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

        val mediaType = "application/json".toMediaType()
        val body =
            "\n{\r\n    \"ListID\": $listId,\r\n   \"MaVaoVien\": \"$maVaoVien\"\r\n}".toRequestBody(
                mediaType
            )
        val request =
            Request.Builder().url("http://bvntp.com:8089/api/hsba/LoadInVanBanKy").post(body)
                .addHeader("Authorization", accessToken ?: "")
                .addHeader("Content-Type", "application/json").build()

        try {
            val call = client.newCall(request)
            val response = call.execute().body
            val buffer = 2048
            val jsonString = BufferedInputStream(response?.byteStream(), buffer)
            val reader: Reader = InputStreamReader(jsonString, "UTF-8")

            val readerJsonReader = JsonReader(reader)

            val fileOutput = BufferedOutputStream(FileOutputStream(file), buffer)

            fun writePdfbyte(reader: JsonReader, writer: BufferedOutputStream) {
                reader.beginArray()
                while (reader.hasNext()) {
                    if (reader.peek() == JsonToken.NUMBER) {
                        writer.write(reader.nextInt())
                    } else {
                        // If it's not an integer, skip the value or handle it accordingly
                        reader.skipValue()
                    }

                }
                reader.endArray()
            }

            readerJsonReader.beginObject()
            while (readerJsonReader.hasNext()) {
                var name = readerJsonReader.nextName()
                if (name.equals("Data")) {
                    if (name!!.contentEquals("null")){
                        throw Exception("Không có dữ liệu hồ sơ bệnh án")
                    }
                    writePdfbyte(readerJsonReader, fileOutput)
                } else {
                    readerJsonReader.skipValue()
                }
            }
            readerJsonReader.endObject()
            reader.close()
            readerJsonReader.close()
            fileOutput.close()
            jsonString.close()
            response?.close()
            onResult(Result.success(file))

        } catch (e: IllegalStateException) {
            val j = IllegalStateException("")
            onResult(Result.failure(j))
        } catch (e: OutOfMemoryError) {
            val j = OutOfMemoryError("Hết bộ nhớ !")
            onResult(Result.failure(j))
        } catch (e: JSONException) {
            val j = JSONException("")
            onResult(Result.failure(j))
        } catch (e: InterruptedException) {
            val i = InterruptedException("Người dùng dừng tải tập tin !")
            onResult(Result.failure(i))
        } catch (e: Exception) {
            onResult((Result.failure(e)))
        } finally {

        }

    }


    fun getLichSuVaoVien(
        context: Context, maBenhNhan: String, onResult: (Result<LichSuDieuTriResponse>) -> Unit
    ) {
        val reqVanBanKy = RequestLichSuDieuTri(MaBN = maBenhNhan)

        val accessToken = retrieveAccessDataDecryptedData(context, "access_token")

        accessToken?.let {
            hoSoBenhAnService.getLichSuDieuTriResponse(accessToken, reqVanBanKy)
                .enqueue(object : Callback<LichSuDieuTriResponse> {
                    override fun onResponse(
                        call: Call<LichSuDieuTriResponse>, response: Response<LichSuDieuTriResponse>
                    ) {
                        if (response.isSuccessful) {
                            val lichSuDieuTriResponse = response.body()
                            lichSuDieuTriResponse?.let {
                                onResult(Result.success(it))
                            }
                        } else {
                            onResult(
                                Result.failure(
                                    throw UnknownError(
                                        response.errorBody().toString()
                                    )
                                )
                            )
                        }
                    }

                    override fun onFailure(call: Call<LichSuDieuTriResponse>, t: Throwable) {
                        onResult(Result.failure(throw UnknownError("Unknown error")))
                    }
                })
        }
    }
}