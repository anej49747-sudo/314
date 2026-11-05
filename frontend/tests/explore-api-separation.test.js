import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'

const reel = readFileSync(new URL('../src/pages/ExplorePage.jsx', import.meta.url), 'utf8')
const api = readFileSync(new URL('../src/api/discovery.js', import.meta.url), 'utf8')
const highlights = readFileSync(new URL('../src/components/RecommendationHighlights.jsx', import.meta.url), 'utf8')

test('Explore uses general discovery and never shows personalised match scores', () => {
  assert.match(reel, /getExploreProjects\(/)
  assert.doesNotMatch(reel, /getRecommendations\(/)
  assert.doesNotMatch(reel, /Your match|project\.score|setPersonalised/)
  assert.match(api, /getExploreProjects[\s\S]*?client\.get\('\/discovery\/explore'/)
})

test('Recommendations retain personalised scores separately', () => {
  assert.match(highlights, /getRecommendations\(/)
  assert.match(highlights, /Your match/)
})
