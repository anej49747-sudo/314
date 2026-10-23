import test from 'node:test'
import assert from 'node:assert/strict'
import { homepageArticles, homepageRecognisedDesigners, homepageFeaturedDetails } from '../src/home/homeRules.js'

test('homepage selects the three latest published articles without mutating the original list', () => {
  const entries = [
    { id: 1, title: 'Old', publishedAt: '2026-09-01T00:00:00Z' },
    { id: 3, title: 'Newest', publishedAt: '2026-10-23T00:00:00Z' },
    { id: 2, title: 'Middle', publishedAt: '2026-10-12T00:00:00Z' },
    { id: 5, title: 'Same day', publishedAt: '2026-10-12T00:00:00Z' },
  ]
  assert.deepEqual(homepageArticles(entries).map(({ id }) => id), [3, 5, 2])
  assert.equal(entries[0].id, 1)
})

test('homepage handles empty and malformed article lists', () => {
  assert.deepEqual(homepageArticles(null), [])
  assert.deepEqual(homepageArticles([null, { id: 1 }]), [])
})

test('recognised designers show only active directory members with awards, sorted by count', () => {
  const designers = [
    { id: 3, name: 'Zeta' }, { id: 1, name: 'Alpha' }, { id: 2, name: 'Beta' }, { id: 4, name: 'Una' },
  ]
  const awards = [
    { designerId: '1', title: 'One' }, { designerId: 3, title: 'One' },
    { designerId: 3, title: 'Two' }, { designerId: 2, title: 'One' },
    { designerId: 999, title: 'Not listed' },
  ]
  assert.deepEqual(homepageRecognisedDesigners(designers, awards).map((d) => d.id), [3, 1, 2])
  assert.deepEqual(homepageRecognisedDesigners([], awards), [])
})

test('featured project joins real project metadata without losing optional cover', () => {
  const selected = { projectId: '16', projectTitle: 'Living Room', coverUrl: '/api/projects/16/images/5' }
  const projects = [{ id: 16, title: 'Living Room', categoryName: null, status: 'CURRENT',
    description: 'Bright renovation', designer: { name: 'Northwood' } }]
  assert.deepEqual(homepageFeaturedDetails(selected, projects), {
    id: '16', title: 'Living Room', designerName: 'Northwood', category: 'Uncategorised',
    description: 'Bright renovation', status: 'CURRENT', coverUrl: '/api/projects/16/images/5',
  })
  assert.equal(homepageFeaturedDetails(null, projects), null)
  assert.equal(homepageFeaturedDetails({ projectId: 3, projectTitle: 'Other' }, [] ).coverUrl, null)
})
