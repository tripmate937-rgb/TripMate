package uk.ac.tees.mad.tripmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import uk.ac.tees.mad.tripmate.data.model.Trip
import uk.ac.tees.mad.tripmate.data.repository.TripRepository

class TripViewModel : ViewModel() {
    private val repository = TripRepository()

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips: StateFlow<List<Trip>> = _trips.asStateFlow()

    private val _currentTrip = MutableStateFlow<Trip?>(null)
    val currentTrip: StateFlow<Trip?> = _currentTrip.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    private val _validationError = MutableStateFlow<ValidationError?>(null)
    val validationError: StateFlow<ValidationError?> = _validationError.asStateFlow()

    init {
        observeTrips()
    }

    private fun observeTrips() {
        viewModelScope.launch {
            repository.getTripsFlow()
                .catch { e ->
                    _error.value = e.message ?: "Failed to load trips"
                }
                .collect { tripList ->
                    _trips.value = tripList
                }
        }
    }

    fun loadTrip(tripId: String) {
        if (tripId == "new") {
            _currentTrip.value = Trip()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getTripById(tripId)
            _isLoading.value = false

            result.onSuccess { trip ->
                _currentTrip.value = trip
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to load trip"
            }
        }
    }

    fun updateTripCoordinates(latitude: Double, longitude: Double) {
        _currentTrip.value = _currentTrip.value?.copy(
            latitude = latitude,
            longitude = longitude
        )
    }

    fun updateTripTitle(title: String) {
        _currentTrip.value = _currentTrip.value?.copy(title = title)
        clearValidationError()
    }

    fun updateTripDestination(destination: String) {
        _currentTrip.value = _currentTrip.value?.copy(destination = destination)
        clearValidationError()
    }

    fun updateTripStartDate(startDate: Long) {
        _currentTrip.value = _currentTrip.value?.copy(startDate = startDate)
        clearValidationError()
    }

    fun updateTripEndDate(endDate: Long) {
        _currentTrip.value = _currentTrip.value?.copy(endDate = endDate)
        clearValidationError()
    }

    fun updateTripActivities(activities: String) {
        _currentTrip.value = _currentTrip.value?.copy(activities = activities)
    }

    private fun validateTrip(trip: Trip): ValidationError? {
        return when {
            trip.title.isBlank() -> ValidationError.EmptyTitle
            trip.title.length < 3 -> ValidationError.TitleTooShort
            trip.destination.isBlank() -> ValidationError.EmptyDestination
            trip.startDate == 0L -> ValidationError.NoStartDate
            trip.endDate == 0L -> ValidationError.NoEndDate
            trip.endDate < trip.startDate -> ValidationError.InvalidDateRange
            else -> null
        }
    }

    fun saveTrip() {
        val trip = _currentTrip.value ?: return

        val validationError = validateTrip(trip)
        if (validationError != null) {
            _validationError.value = validationError
            return
        }

        viewModelScope.launch {
            _isSaving.value = true

            val result = if (trip.id.isEmpty()) {
                repository.addTrip(trip)
            } else {
                repository.updateTrip(trip)
            }

            _isSaving.value = false

            result.onSuccess {
                _saveSuccess.value = true
                _currentTrip.value = null
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to save trip"
            }
        }
    }

    fun deleteTrip(tripId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteTrip(tripId)
            _isLoading.value = false

            result.onSuccess {
                _saveSuccess.value = true
                _currentTrip.value = null
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to delete trip"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearValidationError() {
        _validationError.value = null
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun resetCurrentTrip() {
        _currentTrip.value = null
    }
}

sealed class ValidationError(val message: String) {
    object EmptyTitle : ValidationError("Trip title is required")
    object TitleTooShort : ValidationError("Trip title must be at least 3 characters")
    object EmptyDestination : ValidationError("Destination is required")
    object NoStartDate : ValidationError("Start date is required")
    object NoEndDate : ValidationError("End date is required")
    object InvalidDateRange : ValidationError("End date must be after start date")
}
