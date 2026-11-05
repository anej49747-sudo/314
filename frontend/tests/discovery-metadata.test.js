import assert from 'node:assert/strict'
import test from 'node:test'
import { buildProjectDetails } from '../src/designer/projectCreationRules.js'
import { buildUpdatedProjectDetails } from '../src/designer/projectEditingRules.js'
test('project creation carries structured metadata without changing the existing core fields', () => {
  const project = buildProjectDetails({ title:' Example ', description:' Interior ', status:'CURRENT',categoryId:4,primaryStyleId:2,secondaryStyleIds:[3],propertyTypeId:5,floorAreaSqm:95 })
  assert.equal(project.title,'Example')
  assert.equal(project.categoryId,4)
  assert.deepEqual(project.metadata,{primaryStyleId:2,secondaryStyleIds:[3],propertyTypeId:5,floorAreaSqm:95})
})
test('project editing preserves retained images and metadata', () => {
  const project=buildUpdatedProjectDetails({title:'A',description:'B',status:'COMPLETED',categoryId:null},[11,12])
  assert.deepEqual(project.keepImageIds,[11,12])
  assert.equal(project.metadata, undefined)
})
