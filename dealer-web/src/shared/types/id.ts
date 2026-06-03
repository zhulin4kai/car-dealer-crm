export type EntityId = number | string

export function toRouteId(value: string | string[]): string {
  return Array.isArray(value) ? value[0] ?? '' : value
}
