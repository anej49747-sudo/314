import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import test from 'node:test'
import { questionAudience, showsQuestionPrompt } from '../src/qa/questionAudience.js'

const readSource = (relative) => readFileSync(new URL(relative, import.meta.url), 'utf8')

test('visitor and customer can see the ask prompt; other signed-in roles cannot', () => {
  const cases = [
    [null, 'visitor', true],
    [{ role: undefined }, 'visitor', true],
    [{ role: 'CUSTOMER' }, 'customer', true],
    [{ role: 'INTERIOR_DESIGNER' }, 'other', false],
    [{ role: 'PLATFORM_MANAGER' }, 'other', false],
    [{ role: 'USER_ADMIN' }, 'other', false],
  ]
  for (const [session, audience, visible] of cases) {
    assert.equal(questionAudience(session), audience)
    assert.equal(showsQuestionPrompt(audience), visible)
  }
  assert.equal(showsQuestionPrompt('loading'), false)
})

test('public project Q&A gates the entire ask prompt and adjusts the heading by audience', () => {
  const panel = readSource('../src/qa/ProjectQuestionsPanel.jsx')
  assert.match(panel, /setRole\(questionAudience\(session\)\)/)
  assert.match(panel, /\{showsQuestionPrompt\(role\) && <div className="ld-qa-ask-prompt">/)
  assert.match(panel, /showsQuestionPrompt\(role\) \? 'Find answers about this renovation, or ask the designer something new\.'/)
  assert.match(panel, /role === 'customer' && <form className="ld-qa-ask-form"/)
  assert.match(panel, /Answered questions/)
})

test('designer question workspace fills normal dashboard width and retains responsive two-column layout', () => {
  const css = readSource('../src/qa/projectQuestions.css')
  assert.match(css, /\.ld-qa-designer-page\s*\{[^}]*width: min\(100%, var\(--ld-content, 1180px\)\)/)
  assert.match(css, /\.ld-qa-designer-layout\s*\{[^}]*grid-template-columns:\s*minmax\(235px, 270px\) minmax\(0,1fr\)/)
  assert.match(css, /@media\(max-width:760px\)[\s\S]*?\.ld-qa-designer-layout \{ grid-template-columns:1fr; \}/)
  const designer = readSource('../src/pages/DesignerProjectQuestionsPage.jsx')
  assert.match(designer, /className="ld-qa-stats"/)
  assert.match(designer, /className="ld-qa-filters"/)
  assert.match(designer, /className="ld-qa-queue"/)
})
