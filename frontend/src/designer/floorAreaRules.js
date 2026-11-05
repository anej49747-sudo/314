// PostgreSQL NUMERIC(10,2): eight integer digits, at most two fractional digits.
export const FLOOR_AREA_MAX_SQM = 99999999.99
export const FLOOR_AREA_ERROR = 'Enter a positive floor area in m² (up to two decimal places).'

export function validFloorArea(value) {
  if (value == null || value === '') return true // field is optional
  const number = Number(value)
  return Number.isFinite(number) && number > 0 && number <= FLOOR_AREA_MAX_SQM
    && /^\d+(?:\.\d{1,2})?$/.test(String(value))
}

export function floorAreaValidationRule() {
  return { validator: (_, value) => validFloorArea(value)
    ? Promise.resolve() : Promise.reject(new Error(FLOOR_AREA_ERROR)) }
}
