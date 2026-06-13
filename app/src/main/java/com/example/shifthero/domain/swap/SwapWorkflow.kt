package com.example.shifthero.domain.swap

import com.example.shifthero.core.model.SwapRequest
import com.example.shifthero.core.model.SwapStatus

object SwapWorkflow {
    fun claim(request: SwapRequest, employeeId: String): SwapRequest {
        if (request.status != SwapStatus.Open || request.requesterEmployeeId == employeeId) return request
        return request.copy(accepterEmployeeId = employeeId, status = SwapStatus.Claimed)
    }

    fun approve(request: SwapRequest): SwapRequest {
        if (request.status != SwapStatus.Claimed || request.accepterEmployeeId == null) return request
        return request.copy(status = SwapStatus.Approved)
    }

    fun cancel(request: SwapRequest): SwapRequest {
        if (request.status == SwapStatus.Approved) return request
        return request.copy(status = SwapStatus.Cancelled)
    }
}
