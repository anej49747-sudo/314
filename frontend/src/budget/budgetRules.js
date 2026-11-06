// Story #27: input catalogue and V3 presentation helpers. Monetary calculations are server-side.
export const propertyOptions = [
  { value: 'hdb', label: 'HDB flat' },
  { value: 'condo', label: 'Condominium' },
  { value: 'landed', label: 'Landed property' },
]
export const roomOptions = [
  { id: 'living', name: 'Living & dining', share: 35 },
  { id: 'kitchen', name: 'Kitchen', share: 20 },
  { id: 'bedrooms', name: 'Bedrooms', share: 30 },
  { id: 'bathrooms', name: 'Bathrooms', share: 15 },
]
export const intensityOptions = [
  { value: 'light', label: 'Light refresh' },
  { value: 'standard', label: 'Standard renovation' },
  { value: 'extensive', label: 'Extensive renovation' },
]
export const workOptions = [
  { id: 'painting', name: 'Painting' },
  { id: 'flooring', name: 'Flooring' },
  { id: 'carpentry', name: 'Carpentry' },
  { id: 'electrical', name: 'Electrical work' },
]
export const defaultBudgetChoices = () => ({
  propertyType: 'hdb',
  propertyCondition: 'resale',
  floorAreaSqm: '95',
  roomIntensities: { living: 'standard', kitchen: 'extensive', bedrooms: 'light', bathrooms: 'standard' },
  workTypes: ['painting', 'flooring', 'carpentry', 'electrical'],
  contingencyPercent: 10,
})
export function validateFloorArea(text) {
  if (!/^\d+(?:\.\d{1,2})?$/.test(String(text))) {
    return 'Enter 10 to 1,000 m² using no more than two decimal places.'
  }
  const area = Number(text)
  return Number.isFinite(area) && area >= 10 && area <= 1000
    ? '' : 'Enter 10 to 1,000 m² using no more than two decimal places.'
}
export function validateWorks(rooms, works) {
  if (!Object.values(rooms).some(Boolean)) return 'Select at least one renovation area.'
  if (!works.length) return 'Select at least one type of work.'
  return ''
}
export function budgetRequest(choices) {
  return { ...choices, floorAreaSqm: Number(choices.floorAreaSqm) }
}
export function budgetMoney(value, nearestHundred = false) {
  if (value == null || value === '') return 'S$—'
  const number = Number(value)
  if (!Number.isFinite(number)) return 'S$—'
  const rounded = nearestHundred ? Math.round(number / 100) * 100 : Math.round(number)
  return `S$${rounded.toLocaleString('en-SG')}`
}
export function budgetRange(low, high, nearestHundred = false) {
  return `${budgetMoney(low, nearestHundred)} – ${budgetMoney(high, nearestHundred)}`
}
export function propertyLabel(value) {
  return propertyOptions.find(option => option.value === value)?.label ?? '—'
}
