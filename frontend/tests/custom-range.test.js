import test from 'node:test'
import assert from 'node:assert/strict'
import { validRange, rangeCount, rangeReportCsv } from '../src/report/rangeReportRules.js'

test('inclusive range accepts a single day, cross-month spans and rejects reverse/invalid dates',()=>{
  assert.equal(validRange('2026-09-01','2026-09-01'),true)
  assert.equal(validRange('2026-08-20','2026-10-15'),true)
  assert.equal(validRange('2026-09-03','2026-09-02'),false)
  assert.equal(validRange('2026-02-30','2026-03-01'),false)
})

test('customer and designer period-level unique counts are not the sum of daily unique logins',()=>{
  const report={customer:{logins:9,uniqueUsers:3,newAccounts:1},designer:{logins:4,uniqueUsers:2,newAccounts:1,projectsCreated:6},projectViews:20}
  assert.equal(rangeCount(report,{key:'customerUniqueUsers'}),3)
  assert.equal(rangeCount(report,{key:'designerUniqueUsers'}),2)
  assert.equal(rangeCount(report,{key:'designerProjectsCreated'}),6)
  assert.equal(rangeCount(report,{key:'projectViews'}),20)
})

test('range CSV keeps full daily rows and a deduplicated period total',()=>{
  const report={startDate:'2026-09-01',endDate:'2026-09-01',customer:{logins:3,uniqueUsers:2,newAccounts:1},
    designer:{logins:1,uniqueUsers:1,newAccounts:0,projectsCreated:2},projectViews:7,favouritesAdded:4,reviewsSubmitted:1,engagementComplete:true,
    days:[{date:'2026-09-01',customerLogins:3,customerUniqueUsers:2,customerNewAccounts:1,
      designerLogins:1,designerUniqueUsers:1,designerNewAccounts:0,designerProjectsCreated:2,
      projectViews:7,favouritesAdded:4,reviewsSubmitted:1,engagementComplete:true}]}
  const csv=rangeReportCsv(report)
  assert.match(csv, /"Period total"/)
  assert.match(csv, /"2026-09-01"/)
  assert.ok(csv.trim().split('\r\n').length===3)
})
