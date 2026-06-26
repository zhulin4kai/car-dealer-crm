import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type {
  CreateDeliveryRequest,
  Delivery,
  DeliveryCancelRequest,
  DeliveryCheckItem,
  DeliveryExceptionRequest,
  DeliveryQuery,
  SignDeliveryRequest,
  UpdateDeliveryCheckItemRequest,
} from '@/modules/delivery/model/delivery.types'

export function fetchDeliveryPage(params: DeliveryQuery): Promise<PageResult<Delivery>> {
  return httpClient.get<PageResult<Delivery>>('/api/deliveries', { params })
}

export function fetchDeliveryDetail(id: EntityId): Promise<Delivery> {
  return httpClient.get<Delivery>(`/api/deliveries/${id}`)
}

export function fetchDeliveryCheckItems(id: EntityId): Promise<DeliveryCheckItem[]> {
  return httpClient.get<DeliveryCheckItem[]>(`/api/deliveries/${id}/check-items`)
}

export function createDelivery(data: CreateDeliveryRequest): Promise<Delivery> {
  return httpClient.post<Delivery>('/api/deliveries', data)
}

export function updateDeliveryCheckItem(
  itemId: EntityId,
  data: UpdateDeliveryCheckItemRequest,
): Promise<DeliveryCheckItem> {
  return httpClient.put<DeliveryCheckItem>(`/api/deliveries/check-items/${itemId}`, data)
}

export function signDelivery(id: EntityId, data: SignDeliveryRequest): Promise<Delivery> {
  return httpClient.post<Delivery>(`/api/deliveries/${id}/sign`, data)
}

export function markDeliveryException(
  id: EntityId,
  data: DeliveryExceptionRequest,
): Promise<Delivery> {
  return httpClient.post<Delivery>(`/api/deliveries/${id}/exception`, data)
}

export function cancelDelivery(id: EntityId, data: DeliveryCancelRequest): Promise<Delivery> {
  return httpClient.post<Delivery>(`/api/deliveries/${id}/cancel`, data)
}
