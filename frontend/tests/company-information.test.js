import test from 'node:test'
import assert from 'node:assert/strict'
import { buildCompanyInformationPayload, companyInformationFailureMessage } from '../src/designer/companyInformationRules.js'

test('trims company details and includes only the three approved fields', () => {
  assert.deepEqual(buildCompanyInformationPayload({
    companyName: '  Bright Spaces  ', address: '  Singapore  ', description: '  Design work  ',
    designerId: 123, contactDetails: 'not sent',
  }), { companyName: 'Bright Spaces', address: 'Singapore', description: 'Design work' })
})

test('optional fields can be blank or absent', () => {
  assert.deepEqual(buildCompanyInformationPayload({ companyName: 'Studio' }), {
    companyName: 'Studio', address: '', description: '',
  })
})

test('rejects blank company name and oversized fields', () => {
  assert.throws(() => buildCompanyInformationPayload({ companyName: '   ' }))
  assert.throws(() => buildCompanyInformationPayload({ companyName: 'x'.repeat(151) }))
  assert.throws(() => buildCompanyInformationPayload({ companyName: 'Studio', address: 'x'.repeat(301) }))
  assert.throws(() => buildCompanyInformationPayload({ companyName: 'Studio', description: 'x'.repeat(3001) }))
})

test('returns useful failure feedback', () => {
  assert.match(companyInformationFailureMessage({ response: { status: 400 } }), /correct/)
  assert.match(companyInformationFailureMessage({ response: { status: 403 } }), /cannot update/)
  assert.match(companyInformationFailureMessage({}), /try again/)
})
