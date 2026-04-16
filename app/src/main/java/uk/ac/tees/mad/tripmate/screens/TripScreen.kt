package uk.ac.tees.mad.tripmate.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import android.widget.Toast
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import uk.ac.tees.mad.tripmate.utils.LocationHelper
import uk.ac.tees.mad.tripmate.utils.CalendarHelper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import uk.ac.tees.mad.tripmate.R
import uk.ac.tees.mad.tripmate.data.model.Trip
import uk.ac.tees.mad.tripmate.viewmodel.TripViewModel
import uk.ac.tees.mad.tripmate.viewmodel.ValidationError
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(
    tripId: String,
    onNavigateBack: () -> Unit,
    viewModel: TripViewModel = viewModel()
) {
    val currentTrip by viewModel.currentTrip.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val validationError by viewModel.validationError.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            val tripToSave = currentTrip
            viewModel.resetSaveSuccess()

            if (tripToSave != null && CalendarHelper.hasCalendarPermission(context)) {
                showCalendarDialog = true
            } else {
                onNavigateBack()
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }


    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetCurrentTrip()
        }
    }

    val isNewTrip = tripId == "new"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNewTrip) "Create Trip" else "Edit Trip",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (!isNewTrip) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF673AB7)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isSaving) {
                        viewModel.saveTrip()
                    }
                },
                containerColor = Color(0xFF00BCD4),
                contentColor = Color.White,
                modifier = Modifier.size(64.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF673AB7))
                }
            } else {
                currentTrip?.let { trip ->
                    TripForm(
                        trip = trip,
                        validationError = validationError,
                        onTitleChange = { viewModel.updateTripTitle(it) },
                        onDestinationChange = { viewModel.updateTripDestination(it) },
                        onStartDateChange = { viewModel.updateTripStartDate(it) },
                        onEndDateChange = { viewModel.updateTripEndDate(it) },
                        onActivitiesChange = { viewModel.updateTripActivities(it) },        onCoordinatesChange = { lat, lng -> viewModel.updateTripCoordinates(lat, lng) }

                    )
                }
            }

            if (showCalendarDialog && currentTrip != null) {
                AlertDialog(
                    onDismissRequest = {
                        showCalendarDialog = false
                        viewModel.resetSaveSuccess()
                        onNavigateBack()
                    },
                    title = {
                        Text(
                            "Add to Calendar?",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text("Would you like to add this trip to your device calendar?")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Trip: ${currentTrip!!.title}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val result = CalendarHelper.addTripToCalendar(context, currentTrip!!)
                                if (result > 0) {
                                    Toast.makeText(context, "Added to calendar!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to add to calendar", Toast.LENGTH_SHORT).show()
                                }
                                showCalendarDialog = false
                                viewModel.resetSaveSuccess()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00BCD4)
                            )
                        ) {
                            Text("Yes, Add to Calendar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showCalendarDialog = false
                            viewModel.resetSaveSuccess()
                            onNavigateBack()
                        }) {
                            Text("Skip", color = Color(0xFF673AB7))
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            error?.let { errorMessage ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Text(errorMessage)
                }
            }
            if (showCalendarDialog && currentTrip != null) {
                AlertDialog(
                    onDismissRequest = {
                        showCalendarDialog = false
                        onNavigateBack()
                        viewModel.resetSaveSuccess()
                    },
                    title = { Text("Add to Calendar?") },
                    text = { Text("Would you like to add this trip to your device calendar?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                CalendarHelper.addTripToCalendar(context, currentTrip!!)
                                Toast.makeText(context, "Added to calendar!", Toast.LENGTH_SHORT).show()
                                showCalendarDialog = false
                                onNavigateBack()
                                viewModel.resetSaveSuccess()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00BCD4)
                            )
                        ) {
                            Text("Yes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showCalendarDialog = false
                            onNavigateBack()
                            viewModel.resetSaveSuccess()
                        }) {
                            Text("No", color = Color(0xFF673AB7))
                        }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TripForm(
    trip: Trip,
    validationError: ValidationError?,
    onTitleChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onEndDateChange: (Long) -> Unit,
    onActivitiesChange: (String) -> Unit,
    onCoordinatesChange: (Double, Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00BCD4),
                                        Color(0xFF673AB7)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.location),
                            contentDescription = "Trip",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Trip Details",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF673AB7)
                    )
                }

                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                OutlinedTextField(
                    value = trip.title,
                    onValueChange = onTitleChange,
                    label = { Text("Trip Title") },
                    placeholder = { Text("e.g., Summer Vacation 2024") },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.edit),
                            contentDescription = "Title",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BCD4),
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    isError = validationError is ValidationError.EmptyTitle ||
                            validationError is ValidationError.TitleTooShort
                )

                OutlinedTextField(
                    value = trip.destination,
                    onValueChange = onDestinationChange,
                    label = { Text("Destination") },
                    placeholder = { Text("e.g., Bali, Indonesia") },
                    leadingIcon = {
                        Image(
                            painter = painterResource(id = R.drawable.location),
                            contentDescription = "Destination",
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BCD4),
                        unfocusedBorderColor = Color.LightGray
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    isError = validationError is ValidationError.EmptyDestination
                )

                // Add this RIGHT AFTER the destination OutlinedTextField
                val scope = rememberCoroutineScope()

                Button(
                    onClick = {
                        scope.launch {
                            val location = LocationHelper.getCurrentLocation(context)
                            if (location != null) {
                                val address = LocationHelper.getAddressFromLocation(
                                    context,
                                    location.latitude,
                                    location.longitude
                                )
                                address?.let {
                                    onDestinationChange(it)
                                    onCoordinatesChange(location.latitude, location.longitude)  // ADD THIS LINE
                                }
                                Toast.makeText(context, "Location: $address", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Please enable location permission", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BCD4).copy(alpha = 0.1f),
                        contentColor = Color(0xFF00BCD4)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Use Current Location",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Current Location")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00BCD4),
                                        Color(0xFF673AB7)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.calendar),
                            contentDescription = "Dates",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Travel Dates",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF673AB7)
                    )
                }

                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                DatePickerField(
                    label = "Start Date",
                    selectedDate = trip.startDate,
                    onDateSelected = onStartDateChange,
                    isError = validationError is ValidationError.NoStartDate
                )

                DatePickerField(
                    label = "End Date",
                    selectedDate = trip.endDate,
                    onDateSelected = onEndDateChange,
                    minDate = trip.startDate,
                    isError = validationError is ValidationError.NoEndDate ||
                            validationError is ValidationError.InvalidDateRange
                )

                if (trip.startDate > 0 && trip.endDate > 0) {
                    val days = ((trip.endDate - trip.startDate) / (1000 * 60 * 60 * 24)).toInt() + 1
                    Text(
                        text = "Duration: $days ${if (days == 1) "day" else "days"}",
                        fontSize = 14.sp,
                        color = Color(0xFF00BCD4),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00BCD4),
                                        Color(0xFF673AB7)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.edit),
                            contentDescription = "Activities",
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Activities & Notes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF673AB7)
                    )
                }

                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                OutlinedTextField(
                    value = trip.activities,
                    onValueChange = onActivitiesChange,
                    label = { Text("What do you plan to do?") },
                    placeholder = { Text("Beach activities, sightseeing, restaurants...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00BCD4),
                        unfocusedBorderColor = Color.LightGray
                    ),
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )
            }
        }

        AnimatedVisibility(
            visible = validationError != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        fontSize = 24.sp
                    )
                    Text(
                        text = validationError?.message ?: "",
                        fontSize = 14.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun DatePickerField(
    label: String,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    minDate: Long = 0L,
    isError: Boolean = false
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        if (selectedDate > 0) {
            calendar.timeInMillis = selectedDate
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                onDateSelected(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            if (minDate > 0) {
                datePicker.minDate = minDate
            }
        }
    }

    OutlinedTextField(
        value = if (selectedDate > 0) dateFormat.format(Date(selectedDate)) else "",
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text("Select date") },
        leadingIcon = {
            Image(
                painter = painterResource(id = R.drawable.calendar),
                contentDescription = label,
                modifier = Modifier.size(20.dp)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { datePickerDialog.show() },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00BCD4),
            unfocusedBorderColor = Color.LightGray,
            disabledBorderColor = if (isError) Color.Red else Color.LightGray,
            disabledTextColor = Color.Black
        ),
        enabled = false,
        readOnly = true,
        isError = isError
    )
}

@Composable
private fun DeleteTripDialog(
    trip: Trip,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.delete),
                contentDescription = "Delete",
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Delete Trip?",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to delete \"${trip.title}\"? This action cannot be undone."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF673AB7))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun TripScreenPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Trip Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7)
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.3f))

                        OutlinedTextField(
                            value = "Summer Vacation 2024",
                            onValueChange = {},
                            label = { Text("Trip Title") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00BCD4),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        OutlinedTextField(
                            value = "Bali, Indonesia",
                            onValueChange = {},
                            label = { Text("Destination") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00BCD4),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00BCD4).copy(alpha = 0.1f),
                                contentColor = Color(0xFF00BCD4)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Use Current Location",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Current Location")
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Travel Dates",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7)
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.3f))

                        OutlinedTextField(
                            value = "Jan 15, 2024",
                            onValueChange = {},
                            label = { Text("Start Date") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.LightGray,
                                disabledTextColor = Color.Black
                            )
                        )

                        OutlinedTextField(
                            value = "Jan 22, 2024",
                            onValueChange = {},
                            label = { Text("End Date") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledBorderColor = Color.LightGray,
                                disabledTextColor = Color.Black
                            )
                        )

                        Text(
                            text = "Duration: 7 days",
                            fontSize = 14.sp,
                            color = Color(0xFF00BCD4),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Activities & Notes",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF673AB7)
                        )

                        Divider(color = Color.LightGray.copy(alpha = 0.3f))

                        OutlinedTextField(
                            value = "Beach activities, temple tours, surfing lessons, explore local cuisine",
                            onValueChange = {},
                            label = { Text("What do you plan to do?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00BCD4),
                                unfocusedBorderColor = Color.LightGray
                            ),
                            maxLines = 6
                        )
                    }
                }
            }
        }
    }
}