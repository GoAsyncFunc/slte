package com.slte.app.ui.screen.plans

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.domain.model.PlanInfo
import com.slte.app.domain.repository.OrderRepository
import com.slte.app.ui.ContentPhase
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class PlansData(
    val plans: List<PlanInfo> = emptyList(),
    val phase: ContentPhase = ContentPhase.Loading,
    val isEntering: Boolean = false,
    val errorMessageRes: Int? = null,
)

@HiltViewModel
class PlansViewModel
@Inject
constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _data = MutableStateFlow(PlansData())
    val data: StateFlow<PlansData> = _data.asStateFlow()

    private var loadedOnce = false

    fun enterAndRefresh() {
        loadedOnce = true
        _data.update { it.copy(isEntering = true) }
        loadPlans()
    }

    /** 进程重建恢复到本页时的兜底首拉：正常进入由 enterAndRefresh 负责，这里只在从未加载过时补一次。 */
    fun ensureLoaded() {
        if (loadedOnce) return
        loadedOnce = true
        enterAndRefresh()
    }

    fun retry() {
        loadPlans()
    }

    fun refresh() {
        // Refreshing 沿用现有列表（不闪加载态），与首次加载的 Loading 区分
        if (_data.value.phase == ContentPhase.Refreshing) return
        loadPlans(ContentPhase.Refreshing)
    }

    private fun loadPlans(phase: ContentPhase = ContentPhase.Loading) {
        _data.update { it.copy(phase = phase, errorMessageRes = null) }
        viewModelScope.launch {
            orderRepository.fetchPlans().fold(
                onSuccess = { plans ->
                    _data.update {
                        it.copy(
                            plans = plans.filter { p -> p.show },
                            phase = ContentPhase.Idle,
                            isEntering = false,
                            errorMessageRes = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    _data.update {
                        it.copy(
                            phase = ContentPhase.Idle,
                            isEntering = false,
                            errorMessageRes = ErrorMessages.forOrder(throwable),
                        )
                    }
                },
            )
        }
    }
}
