package dev.notyouraverage.otpcourier.composables

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.notyouraverage.otpcourier.R
import dev.notyouraverage.otpcourier.constants.Constants
import dev.notyouraverage.otpcourier.managers.DataStoreManager
import dev.notyouraverage.otpcourier.managers.smsCourierPreferenceDataStore
import dev.notyouraverage.otpcourier.services.foreground.MasterService
import dev.notyouraverage.otpcourier.utils.loadSavedPreferences
import dev.notyouraverage.otpcourier.utils.savePreferences
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val dataStoreManager = DataStoreManager(context)

    var secretPassword by remember {
        mutableStateOf("")
    }
    var whiteListedContactNumber by remember {
        mutableStateOf("")
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        loadSavedPreferences(context.smsCourierPreferenceDataStore) {
            secretPassword = it.secretPassword
            whiteListedContactNumber = it.whiteListedContactNumber
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.home), contentDescription = "Login Image",
            modifier = Modifier.size(250.dp)
        )

        Text(text = "Foreground Service", fontWeight = FontWeight.Bold, fontSize = 20.sp)

        Row {
            Button(
                onClick = {
                    if (secretPassword == "" || whiteListedContactNumber == "") {
                        Toast.makeText(
                            context,
                            "Please set the secret app preferences",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Intent(context, MasterService::class.java).also {
                            it.setAction(MasterService.START_SELF)
                            it.putExtra(Constants.SECRET_PASSWORD, secretPassword)
                            it.putExtra(
                                Constants.WHITE_LISTED_CONTACT_NUMBER,
                                whiteListedContactNumber
                            )
                            context.startService(it)
                        }
                    }

                },
                modifier = Modifier
                    .width(140.dp)
                    .height(50.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Text(text = "Start Service", fontSize = 15.sp)
            }

            Button(
                onClick = {

                    Intent(context, MasterService::class.java).also {
                        it.setAction(MasterService.STOP_SELF)
                        context.startService(it)
                    }
                },
                modifier = Modifier
                    .width(150.dp)
                    .height(50.dp)
                    .clip(
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Text(text = "Stop Service", fontSize = 15.sp)
            }

        }
        Button(onClick = { /*TODO*/ }) {
            Text(text = "Read SMS")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = secretPassword,
            onValueChange = {
                secretPassword = it
            },
            label = {
                Text(text = "Secret Password")
            })

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = whiteListedContactNumber,
            onValueChange = {
                whiteListedContactNumber = it
            },
            label = {
                Text(text = "Whitelisted Contact Number")
            })


        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            scope.launch {
                savePreferences(
                    secretPassword,
                    whiteListedContactNumber,
                    dataStoreManager
                )
                Toast.makeText(context, "Successfully Updated Preferences", Toast.LENGTH_SHORT)
                    .show()
            }
        }) {
            Text(text = "Save Preferences")
        }

    }
}


