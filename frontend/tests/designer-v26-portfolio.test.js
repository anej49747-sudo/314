import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { validFloorArea, FLOOR_AREA_MAX_SQM } from '../src/designer/floorAreaRules.js'
import { buildProjectDetails } from '../src/designer/projectCreationRules.js'
import { buildUpdatedProjectDetails } from '../src/designer/projectEditingRules.js'

const read = (file) => readFileSync(new URL(`../src/${file}`, import.meta.url), 'utf8')

test('floor area is optional, positive and limited to numeric(10,2)', () => {
  for (const value of [null, undefined, '', '1', '0.01', 218, '218.00', '99999999.99'])
    assert.equal(validFloorArea(value), true, `valid: ${value}`)
  for (const value of ['abc', '-10', '0', '0.00', '1.123', '1e5', '100000000', Infinity, '5m²', '23,5'])
    assert.equal(validFloorArea(value), false, `invalid: ${value}`)
  assert.equal(FLOOR_AREA_MAX_SQM, 99999999.99)
})

test('create/edit payload validates floor area rather than silently clearing it', () => {
  const fields = { title: 'Example', description: 'Text', status: 'CURRENT', floorAreaSqm: '218.00' }
  assert.equal(buildProjectDetails(fields).metadata.floorAreaSqm, '218.00')
  assert.equal(buildUpdatedProjectDetails(fields, [4]).metadata.floorAreaSqm, '218.00')
  assert.equal(buildProjectDetails({ ...fields, floorAreaSqm: '' }).metadata.floorAreaSqm, null)
  assert.throws(() => buildProjectDetails({ ...fields, floorAreaSqm: 'anything' }), /positive floor area/)
  assert.throws(() => buildUpdatedProjectDetails({ ...fields, floorAreaSqm: '-1' }, []), /positive floor area/)
})

test('portfolio forms share strict area field; editing requires metadata loading', () => {
  const metadata = read('components/ProjectMetadataFields.jsx')
  assert.match(metadata, /floorAreaValidationRule\(\)/)
  assert.match(metadata, /inputMode="decimal"/)
  const editor = read('pages/EditPortfolioProjectPage.jsx')
  assert.match(editor, /const loadedMetadata = await getProjectMetadata\(chosen.id, true\)/)
  assert.doesNotMatch(editor, /catch \{ setMetadata\(null\) \}/)
  assert.match(editor, /setMetadata\(loadedMetadata\)/)
})

test('portfolio edit photo grid has bounded thumbnails and no single-photo stretching', () => {
  const css = read('ui/designerV26.css')
  assert.match(css, /grid-template-columns:repeat\(auto-fill,minmax\(175px,220px\)\)/)
  assert.match(css, /object-fit:contain/)
  assert.match(read('pages/EditPortfolioProjectPage.jsx'), /Remove photo/)
})

test('designer dashboard and portfolio share card layout with customer favourites', () => {
  for (const page of ['DesignerPage.jsx', 'DesignerPortfolioPage.jsx'])
    assert.match(read(`pages/${page}`), /<DesignerProjectCard/)
  const designerCard = read('components/DesignerProjectCard.jsx')
  assert.match(designerCard, /<ProjectCardLayout/)
  assert.match(designerCard, /Project views/)
  assert.match(designerCard, /Delete project/)
  assert.match(designerCard, /Edit project/)
  const saved = read('components/SavedProjectCard.jsx')
  assert.match(saved, /<ProjectCardLayout/)
  const layout = read('components/ProjectCardLayout.jsx')
  assert.match(layout, /<ProjectAttributes project=\{project\}/)
  assert.match(layout, /project\.images\?\.\[0\]/)
})

test('portfolio response adds metadata while preserving the existing counts', () => {
  const dto = readFileSync(new URL('../../backend/src/main/java/csit314/renovation/portfolio/dto/PortfolioProjectResponse.java', import.meta.url), 'utf8')
  const service = readFileSync(new URL('../../backend/src/main/java/csit314/renovation/portfolio/control/PortfolioService.java', import.meta.url), 'utf8')
  assert.match(dto, /long viewCount, long favouriteCount, Map<String,Object> metadata/)
  assert.match(dto, /long viewCount, long favouriteCount\) \{/)
  assert.match(dto, /return from\(project,images,viewCount,favouriteCount,Map\.of\(\)\)/)
  assert.match(service, /metadataForProjects\(ids\)/)
  assert.match(service, /counts\.viewCount\(\), counts\.favouriteCount\(\)/)
})
