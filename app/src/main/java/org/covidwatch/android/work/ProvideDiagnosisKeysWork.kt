package org.covidwatch.android.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.google.common.io.BaseEncoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.covidwatch.android.data.asCovidExposureConfiguration
import org.covidwatch.android.data.diagnosiskeystoken.DiagnosisKeysToken
import org.covidwatch.android.data.diagnosiskeystoken.DiagnosisKeysTokenRepository
import org.covidwatch.android.data.keyfile.KeyFile
import org.covidwatch.android.data.keyfile.KeyFileRepository
import org.covidwatch.android.data.positivediagnosis.PositiveDiagnosisRepository
import org.covidwatch.android.data.pref.PreferenceStorage
import org.covidwatch.android.exposurenotification.ENStatus
import org.covidwatch.android.exposurenotification.ExposureNotificationManager
import org.covidwatch.android.extension.failure
import org.covidwatch.android.ui.Notifications
import org.covidwatch.android.ui.Notifications.Companion.DOWNLOAD_REPORTS_NOTIFICATION_ID
import org.koin.java.KoinJavaComponent.inject
import timber.log.Timber
import java.security.SecureRandom

class ProvideDiagnosisKeysWork(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val enManager by inject(ExposureNotificationManager::class.java)
    private val diagnosisRepository by inject(PositiveDiagnosisRepository::class.java)
    private val diagnosisKeysTokenRepository by inject(DiagnosisKeysTokenRepository::class.java)
    private val notifications by inject(Notifications::class.java)
    private val preferences by inject(PreferenceStorage::class.java)
    private val keyFileRepository by inject(KeyFileRepository::class.java)

    private val base64 = BaseEncoding.base64()
    private val randomTokenByteLength = 32
    private val secureRandom: SecureRandom = SecureRandom()

    private fun randomToken(): String {
        val bytes = ByteArray(randomTokenByteLength)
        secureRandom.nextBytes(bytes)
        return base64.encode(bytes)
    }

    override suspend fun doWork(): Result {
        setForeground(
            ForegroundInfo(
                DOWNLOAD_REPORTS_NOTIFICATION_ID,
                notifications.downloadingReportsNotification()
            )
        )
        return withContext(Dispatchers.IO) {
            try {
                val diagnosisKeys = diagnosisRepository.diagnosisKeys()
                Timber.d("Adding ${diagnosisKeys.size} batches of diagnoses to EN framework")

                val token = randomToken()
                val exposureConfiguration = preferences.exposureConfiguration
                diagnosisKeys.filter { it.keys.isNotEmpty() }.forEach { fileBatch ->
                    val keys = fileBatch.keys
                    enManager.provideDiagnosisKeys(keys, token, exposureConfiguration).apply {
                        success {
                            Timber.d("Added keys to EN with token: $token")
                            val dir = keys[0].parentFile
                            keys.forEachIndexed { i, file ->
                                keyFileRepository.add(
                                    KeyFile(
                                        fileBatch.region,
                                        fileBatch.batch,
                                        file,
                                        fileBatch.urls[i]
                                    )
                                )
                                file.delete()
                            }
                            dir?.delete()
                        }
                        failure {
                            Timber.d("Failed to added keys to EN")
                            return@withContext failure(it)
                        }
                    }
                }

                diagnosisKeysTokenRepository.insert(
                    DiagnosisKeysToken(
                        token,
                        exposureConfiguration = exposureConfiguration.asCovidExposureConfiguration()
                    )
                )
                Result.success()
            } catch (e: Exception) {
                Timber.e(e)
                failure(ENStatus(e))
            }
        }
    }
}