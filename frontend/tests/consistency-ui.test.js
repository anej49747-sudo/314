import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { customerLoginDestination } from '../src/project/favouriteRules.js'

const source = path => readFileSync(new URL(`../src/${path}`, import.meta.url), 'utf8')

test('dashboard actions are personal tools, with security replacing Explore', () => {
  const page = source('pages/CustomerPage.jsx')
  const cards = page.slice(page.indexOf('const shortcuts = ['), page.indexOf('export default'))
  assert.match(cards, /Account security/)
  assert.match(cards, /My preferences/)
  assert.doesNotMatch(cards, /Explore projects/)
  assert.match(page, /My dashboard/)
})

test('Explore toggles saved projects, preserves the vertical reel and has no manual load-more', () => {
  const page = source('pages/ExplorePage.jsx')
  assert.match(page, /removeFavourite\(project\.id\)/)
  assert.match(page, /saveFavourite\(project\.id\)/)
  assert.match(page, /aria-pressed=\{Boolean\(project\.saved\)\}/)
  assert.match(page, /discovery-reel-feed/)
  assert.doesNotMatch(page, />Load more projects</)
  assert.doesNotMatch(page, /Your match/)
})

test('Explore login return is safe and outside redirects are still rejected', () => {
  assert.equal(customerLoginDestination({ returnTo: '/explore' }), '/explore')
  assert.equal(customerLoginDestination({ returnTo: '/explore?next=//other.site' }), '/customer')
  assert.equal(customerLoginDestination({ returnTo: '//other.site' }), '/customer')
})

test('Projects filters include all four classifications and all removable active chips', () => {
  const page = source('pages/ProjectSearchPage.jsx')
  assert.match(page, /activeProjectFilters\(/)
  assert.match(page, /chips\.map\(/)
  assert.match(page, /ProjectAttributes project=\{project\}/)
  assert.doesNotMatch(page, /className="project-search-category"/)
})
