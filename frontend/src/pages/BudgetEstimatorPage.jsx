import { Alert, Button, Spin } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { currentCustomer } from '../api/authentication.js'
import { estimateBudget } from '../api/budget.js'
import { accountAreaPath } from '../auth/headerSessionRules.js'
import CustomerReturnButton from '../components/CustomerReturnButton.jsx'
import {
  budgetMoney, budgetRange, budgetRequest, defaultBudgetChoices, intensityOptions,
  propertyLabel, propertyOptions, roomOptions, validateFloorArea, validateWorks, workOptions,
} from '../budget/budgetRules.js'
import '../ui/budgetEstimator.css'

const stepLabels = [
  ['Property details', 'Tell us about your home'],
  ['Renovation works', 'Choose areas and work'],
  ['Budget estimate', 'Review and adjust'],
]

export default function BudgetEstimatorPage() {
  const navigate = useNavigate()
  const [authorized, setAuthorized] = useState(false)
  const [loading, setLoading] = useState(true)
  const [authError, setAuthError] = useState('')
  const [step, setStep] = useState(1)
  const [choices, setChoices] = useState(defaultBudgetChoices)
  const [areaError, setAreaError] = useState('')
  const [workError, setWorkError] = useState('')
  const [estimateError, setEstimateError] = useState('')
  const [calculating, setCalculating] = useState(false)
  const [result, setResult] = useState(null)
  const [detailsOpen, setDetailsOpen] = useState(false)
  const requestSequence = useRef(0)

  useEffect(() => {
    let active = true
    currentCustomer().then(profile => {
      if (!active) return
      if (profile.role !== 'CUSTOMER') navigate(accountAreaPath(profile) || '/login', { replace: true })
      else setAuthorized(true)
    }).catch(error => {
      if (!active) return
      if (error.response?.status === 401) navigate('/login', { replace: true, state: { returnTo: '/customer/budget' } })
      else if (error.response?.status === 403) setAuthError('This estimator is available to customer accounts only.')
      else setAuthError('Unable to verify your account. Please try again.')
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [navigate])

  // Debounced server recalculation on the approved V3 contingency slider.
  useEffect(() => {
    if (step !== 3 || !result || choices.contingencyPercent === result.contingencyPercent) return undefined
    const sequence = ++requestSequence.current
    const timer = setTimeout(async () => {
      setCalculating(true)
      setEstimateError('')
      try {
        const updated = await estimateBudget(budgetRequest(choices))
        if (sequence === requestSequence.current) setResult(updated)
      } catch (error) {
        if (sequence !== requestSequence.current) return
        if (error.response?.status === 401) navigate('/login', { replace: true, state: { returnTo: '/customer/budget' } })
        else setEstimateError(error.response?.data?.message || 'Unable to update your estimate. Please try again.')
      } finally {
        if (sequence === requestSequence.current) setCalculating(false)
      }
    }, 220)
    return () => { clearTimeout(timer) }
  }, [step, result, choices, navigate])

  function update(key, value) {
    setChoices(previous => ({ ...previous, [key]: value }))
    setEstimateError('')
  }
  function toggleRoom(room) {
    setChoices(previous => {
      const rooms = { ...previous.roomIntensities }
      if (rooms[room]) delete rooms[room]
      else rooms[room] = 'standard'
      return { ...previous, roomIntensities: rooms }
    })
    setWorkError('')
  }
  function updateIntensity(room, intensity) {
    setChoices(previous => ({ ...previous, roomIntensities: { ...previous.roomIntensities, [room]: intensity } }))
  }
  function toggleWork(work) {
    setChoices(previous => ({ ...previous, workTypes: previous.workTypes.includes(work)
      ? previous.workTypes.filter(value => value !== work) : [...previous.workTypes, work] }))
    setWorkError('')
  }
  function nextFromProperty() {
    const error = validateFloorArea(choices.floorAreaSqm)
    setAreaError(error)
    if (!error) setStep(2)
  }
  async function calculate() {
    const error = validateWorks(choices.roomIntensities, choices.workTypes)
    setWorkError(error)
    if (error) return
    const area = validateFloorArea(choices.floorAreaSqm)
    if (area) { setAreaError(area); setStep(1); return }
    const sequence = ++requestSequence.current
    setCalculating(true)
    setEstimateError('')
    try {
      const response = await estimateBudget(budgetRequest(choices))
      if (sequence !== requestSequence.current) return
      setResult(response)
      setDetailsOpen(false)
      setStep(3)
    } catch (failure) {
      if (failure.response?.status === 401) navigate('/login', { replace: true, state: { returnTo: '/customer/budget' } })
      else setEstimateError(failure.response?.data?.message || 'Could not calculate your estimate. Please try again.')
    } finally { if (sequence === requestSequence.current) setCalculating(false) }
  }
  function reset() {
    ++requestSequence.current
    setChoices(defaultBudgetChoices())
    setStep(1)
    setResult(null)
    setDetailsOpen(false)
    setAreaError('')
    setWorkError('')
    setEstimateError('')
    setCalculating(false)
  }

  return <section className="ld-budget-page" aria-labelledby="ld-budget-heading">
    <CustomerReturnButton />
    <p className="ld-budget-eyebrow">Renovation planning</p>
    <h1 id="ld-budget-heading">Budget estimator</h1>
    <p className="ld-budget-intro">Build a rough renovation budget in three simple steps. Choose your property, select the areas and work involved, then review your estimated cost range.</p>
    <div className="ld-budget-notice" role="note"><strong>Illustrative planning model.</strong> All rates, room-area percentages and cost factors are demonstration assumptions, not verified Singapore renovation prices or contractor quotations. The range is not a statistical confidence interval.</div>

    {loading && <div role="status" className="ld-budget-loading"><Spin /> Checking your account…</div>}
    {authError && <Alert type="error" showIcon message={authError} />}
    {authorized && <>
      <div className="ld-budget-steps" aria-label="Estimation progress">
        {stepLabels.map(([name, caption], index) => {
          const number = index + 1
          return <button key={name} type="button" className={`ld-budget-step ${step === number ? 'active' : number < step ? 'complete' : ''}`}
            aria-current={step === number ? 'step' : undefined}
            disabled={number > step || calculating}
            onClick={() => { if (number < step) { ++requestSequence.current; setStep(number); setEstimateError('') } }}>
            <span className="ld-budget-step-number">{number}</span><span><strong>{name}</strong><small>{caption}</small></span>
          </button>
        })}
      </div>
      {step === 1 && <div className="ld-budget-columns">
        <section className="ld-budget-panel" aria-labelledby="ld-budget-property-heading">
          <h2 id="ld-budget-property-heading">Tell us about your property</h2><p className="ld-budget-support">These details help shape your indicative estimate.</p>
          <div className="ld-budget-field"><label htmlFor="ld-budget-property">Property type</label>
            <select id="ld-budget-property" value={choices.propertyType} onChange={event => update('propertyType', event.target.value)}>
              {propertyOptions.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
            </select></div>
          <fieldset className="ld-budget-condition"><legend>Property condition</legend><div className="ld-budget-condition-cards">
            {[['new', 'New / BTO', 'Newly completed or minimally fitted property'], ['resale', 'Resale / existing', 'Existing finishes or fittings may need removal']].map(([value, title, description]) =>
              <label key={value} className="ld-budget-option"><input type="radio" name="budget-condition" value={value}
                checked={choices.propertyCondition === value} onChange={() => update('propertyCondition', value)} />
                <span><strong>{title}</strong><small>{description}</small></span></label>)}
          </div></fieldset>
          <div className="ld-budget-field"><label htmlFor="ld-budget-area">Floor area (m²)</label>
            <input id="ld-budget-area" type="number" min="10" max="1000" step="0.01" inputMode="decimal"
              value={choices.floorAreaSqm} aria-invalid={Boolean(areaError)} aria-describedby="ld-budget-area-hint ld-budget-area-error"
              onChange={event => { update('floorAreaSqm', event.target.value); setAreaError('') }} />
            <small id="ld-budget-area-hint">10–1,000 m², up to two decimal places.</small>
            <small id="ld-budget-area-error" className="ld-budget-error" role="alert">{areaError}</small></div>
          <div className="ld-budget-actions"><Button type="primary" onClick={nextFromProperty}>Continue to renovation works →</Button></div>
        </section>
        <aside className="ld-budget-sidebar"><span className="ld-budget-stamp">STEP 1 OF 3</span><h3>What we'll use</h3>
          <div className="ld-budget-side-item"><span>Property type</span><strong>{propertyLabel(choices.propertyType)}</strong></div>
          <div className="ld-budget-side-item"><span>Condition</span><strong>{choices.propertyCondition === 'new' ? 'New / BTO' : 'Resale / existing'}</strong></div>
          <div className="ld-budget-side-item"><span>Floor area</span><strong>{choices.floorAreaSqm || '—'} m²</strong></div>
          <p>We calculate your estimate without saving a budget history.</p><hr /><h3>No quotations or obligations</h3><p>Contractors must confirm actual scope, materials and pricing.</p>
        </aside>
      </div>}
      {step === 2 && <div className="ld-budget-columns">
        <section className="ld-budget-panel" aria-labelledby="ld-budget-work-heading"><h2 id="ld-budget-work-heading">Choose your renovation works</h2>
          <p className="ld-budget-support">Select the rooms you plan to renovate and the approximate intensity of the work.</p>
          <h3 className="ld-budget-section-heading">Areas to renovate</h3>
          <p className="ld-budget-support">Area shares below are placeholder assumptions, not actual room measurements.</p>
          <div className="ld-budget-room-grid">{roomOptions.map(room => <div key={room.id} className={`ld-budget-room ${choices.roomIntensities[room.id] ? 'selected' : ''}`}>
            <label className="ld-budget-check"><input type="checkbox" checked={Boolean(choices.roomIntensities[room.id])}
              onChange={() => toggleRoom(room.id)} /><span><strong>{room.name}</strong><small>Approx. {room.share}% of property area</small></span></label>
            <div className="ld-budget-field"><label htmlFor={`ld-budget-intensity-${room.id}`}>Work intensity</label>
              <select id={`ld-budget-intensity-${room.id}`} disabled={!choices.roomIntensities[room.id]}
                value={choices.roomIntensities[room.id] || 'standard'} onChange={event => updateIntensity(room.id, event.target.value)}>
                {intensityOptions.map(option => <option key={option.value} value={option.value}>{option.label}</option>)}
              </select></div></div>)}</div>
          <hr /><h3 className="ld-budget-section-heading">Types of work</h3>
          <p className="ld-budget-support">Independent work categories applied once to each selected area.</p>
          <div className="ld-budget-work-grid">{workOptions.map(work => <label key={work.id} className="ld-budget-work-choice">
            <input type="checkbox" checked={choices.workTypes.includes(work.id)} onChange={() => toggleWork(work.id)} />{work.name}</label>)}</div>
          <hr /><div className="ld-budget-field ld-budget-contingency-field"><label htmlFor="ld-budget-contingency">Contingency allowance</label>
            <select id="ld-budget-contingency" value={choices.contingencyPercent} onChange={event => update('contingencyPercent', Number(event.target.value))}>
              {[0, 5, 10, 15, 20, 25, 30].map(value => <option key={value} value={value}>{value}%{value === 10 ? ' — suggested example' : ''}</option>)}
            </select><small>Added on top of the base estimate.</small></div>
          {workError && <Alert type="error" showIcon message={workError} />}
          {estimateError && <Alert type="error" showIcon message={estimateError} />}
          <div className="ld-budget-actions"><Button onClick={() => setStep(1)} disabled={calculating}>← Property details</Button>
            <Button type="primary" onClick={calculate} loading={calculating}>Calculate estimate →</Button></div>
        </section>
        <aside className="ld-budget-sidebar"><span className="ld-budget-stamp">STEP 2 OF 3</span><h3>Current property</h3>
          <div className="ld-budget-side-item"><span>Property</span><strong>{propertyLabel(choices.propertyType)} · {choices.propertyCondition === 'new' ? 'New' : 'Resale'}</strong></div>
          <div className="ld-budget-side-item"><span>Floor area</span><strong>{choices.floorAreaSqm} m²</strong></div><hr />
          <h3>Example cost methodology</h3><p>Each work type uses a sample midpoint rate with a narrow indicative band. Room shares, intensity and property/condition factors adjust the calculation.</p>
          <p>Rates and assumptions are demonstration values, not verified market prices.</p></aside>
      </div>}
      {step === 3 && result && <section aria-labelledby="ld-budget-result-heading">
        <div className="ld-budget-result-hero" aria-live="polite" aria-atomic="true">
          <h2 id="ld-budget-result-heading">Your estimated renovation budget</h2>
          <div className="ld-budget-result-amount">{budgetMoney(result.midpoint, true)}</div>
          <p>Illustrative planning midpoint, including contingency</p>
          <div className="ld-budget-result-range">Indicative range: <strong>{budgetRange(result.rangeLow, result.rangeHigh, true)}</strong></div>
          <p className="ld-budget-result-explain">Based on your selected property and renovation works. Sample assumptions only — not a contractor quotation or statistically validated price range.</p>
        </div>
        <div className="ld-budget-result-actions"><Button onClick={() => { ++requestSequence.current; setStep(2); setEstimateError('') }}>← Change renovation works</Button>
          <Button type="primary" onClick={reset}>Start a new estimate</Button></div>
        {estimateError && <Alert type="error" showIcon message={estimateError} />}
        {calculating && <p className="ld-budget-updating" role="status">Updating estimate…</p>}
        <details className="ld-budget-disclosure" open={detailsOpen} onToggle={event => setDetailsOpen(event.currentTarget.open)}>
          <summary>View cost breakdown and assumptions</summary>
          <div className="ld-budget-disclosure-body"><div className="ld-budget-breakdown-totals">
            <div><small>Base renovation cost</small><strong>{budgetRange(result.baseLow, result.baseHigh)}</strong></div>
            <div><small>Contingency allowance</small><strong>{budgetRange(result.contingencyLow, result.contingencyHigh)}</strong></div>
          </div><div className="ld-budget-result-columns">
            <div><h3>Cost breakdown by area</h3><p className="ld-budget-muted">Based on sample room allocations and selected work types.</p>
              <div className="ld-budget-table-scroll"><table><thead><tr><th>Renovation area</th><th>Area coverage</th><th>Base cost range</th></tr></thead>
                <tbody>{result.rooms.map(room => <tr key={room.id}><td><strong>{room.name}</strong><small>{room.intensity} intensity</small></td>
                  <td>{Number(room.areaSqm).toLocaleString('en-SG', { maximumFractionDigits: 2 })} m²</td>
                  <td>{budgetRange(room.baseLow, room.baseHigh)}</td></tr>)}</tbody>
                <tfoot><tr><td>Base total</td><td></td><td>{budgetRange(result.baseLow, result.baseHigh)}</td></tr></tfoot>
              </table></div>
            </div><div><h3>Your assumptions</h3>
              {[
                ['Property', `${propertyLabel(result.propertyType)} · ${result.propertyCondition === 'new' ? 'New' : 'Resale'}`],
                ['Floor area', `${result.floorAreaSqm} m²`],
                ['Renovation areas', `${result.rooms.length} selected area${result.rooms.length === 1 ? '' : 's'}`],
                ['Property adjustment', `×${Number(result.propertyFactor).toFixed(2)}`],
                ['Condition adjustment', `×${Number(result.conditionFactor).toFixed(2)}`],
                ['Work types', result.workRates.map(work => work.name).join(', ')],
              ].map(([label, value]) => <div className="ld-budget-assumption" key={label}><span>{label}</span><strong>{value}</strong></div>)}
              <div className="ld-budget-slider"><label htmlFor="ld-budget-result-contingency">Adjust contingency</label>
                <output htmlFor="ld-budget-result-contingency">{choices.contingencyPercent}%</output>
                <input id="ld-budget-result-contingency" type="range" min="0" max="30" step="5"
                  value={choices.contingencyPercent} onChange={event => update('contingencyPercent', Number(event.target.value))} />
              </div>
            </div>
          </div>
          <details className="ld-budget-rate-details"><summary>View sample rates and calculation method</summary>
            <p>Illustrative planning rates per m² (not verified Singapore prices):</p>
            <table><tbody>{result.workRates.map(work => <tr key={work.id}><td>{work.name}</td><td>{budgetRange(work.rateLow, work.rateHigh)}/m²</td></tr>)}</tbody></table>
            <p>Per room: total area × estimated room share × intensity × room factor × selected work rates × property factor × condition factor. Sum base costs, then apply contingency.</p>
            <p>Assumed room shares: living/dining 35%, kitchen 20%, bedrooms 30%, bathrooms 15%. No budget history is saved.</p>
          </details><p className="ld-budget-muted">Exclusions: consultation fees, permits, appliances, specialist structural work and unforeseen site conditions.</p>
          </div>
        </details>
      </section>}
    </>}
  </section>
}
