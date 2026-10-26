import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import '../report/dateRangeCalendar.css'

const asDate = iso => new Date(`${iso}T00:00:00Z`)
const isoDate = date => date.toISOString().slice(0, 10)
const shiftMonth = (first, n) => new Date(Date.UTC(first.getUTCFullYear(), first.getUTCMonth() + n, 1))
const monthTitle = first => new Intl.DateTimeFormat('en-SG', { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(first)
const pretty = date => date ? new Intl.DateTimeFormat('en-SG', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' }).format(asDate(date)) : 'Select date'

/** Two-click inclusive calendar range: one click selects one day, next click sets the range end. */
export default function DateRangeCalendar({ startDate, endDate, onChange }) {
  const [open, setOpen] = useState(false)
  const [selectingEnd, setSelectingEnd] = useState(false)
  const [month, setMonth] = useState(() => new Date(Date.UTC(Number(startDate.slice(0, 4)), Number(startDate.slice(5, 7)) - 1, 1)))
  const rootRef = useRef(null)
  useEffect(() => {
    if (!open) return undefined
    const onDown = event => { if (!rootRef.current?.contains(event.target)) setOpen(false) }
    const onKey = event => { if (event.key === 'Escape') setOpen(false) }
    document.addEventListener('pointerdown', onDown)
    document.addEventListener('keydown', onKey)
    return () => { document.removeEventListener('pointerdown', onDown); document.removeEventListener('keydown', onKey) }
  }, [open])

  const firstWeekday = (month.getUTCDay() + 6) % 7
  const last = new Date(Date.UTC(month.getUTCFullYear(), month.getUTCMonth() + 1, 0)).getUTCDate()
  function choose(day) {
    if (!selectingEnd || !startDate || day < startDate) {
      onChange(day, day)
      setSelectingEnd(true)
    } else {
      onChange(startDate, day)
      setSelectingEnd(false)
      setOpen(false)
    }
  }
  return <div ref={rootRef} className="ld-range-picker">
    <button type="button" className="ld-range-trigger" onClick={() => { setOpen(v => !v); setSelectingEnd(false); setMonth(new Date(Date.UTC(Number(startDate.slice(0, 4)), Number(startDate.slice(5, 7)) - 1, 1))) }}
      aria-expanded={open} aria-haspopup="dialog">
      <CalendarDays size={19} aria-hidden="true" />
      <span><strong>{pretty(startDate)}</strong><small>Check-in style start</small></span>
      <span className="ld-range-separator">→</span>
      <span><strong>{pretty(endDate)}</strong><small>Inclusive end</small></span>
    </button>
    {open && <div className="ld-range-popover" role="dialog" aria-label="Choose reporting date range">
      <div className="ld-range-head">
        <button type="button" aria-label="Previous month" onClick={() => setMonth(m => shiftMonth(m, -1))}><ChevronLeft size={19}/></button>
        <strong>{monthTitle(month)}</strong>
        <button type="button" aria-label="Next month" onClick={() => setMonth(m => shiftMonth(m, 1))}><ChevronRight size={19}/></button>
      </div>
      <p className="ld-range-hint">{selectingEnd ? 'Select the final day (inclusive)' : 'Select the first day; select it again for a one-day report'}</p>
      <div className="ld-range-weekdays">{['M','T','W','T','F','S','S'].map((d,i)=><span key={i}>{d}</span>)}</div>
      <div className="ld-range-days">{Array.from({length:firstWeekday},(_,i)=><span key={`pad${i}`} />)}
        {Array.from({length:last},(_,i)=> {
          const day=isoDate(new Date(Date.UTC(month.getUTCFullYear(),month.getUTCMonth(),i+1)))
          const selected = day===startDate||day===endDate
          const between = day>=startDate && day<=endDate
          return <button key={day} type="button" className={`${selected?'selected ':''}${between?'in-range':''}`}
            aria-label={day} aria-pressed={selected} onClick={() => choose(day)}>{i+1}</button>
        })}</div>
      <div className="ld-range-manual">
        <label>From<input type="date" value={startDate} onChange={e => { if(e.target.value) {onChange(e.target.value, e.target.value > endDate ? e.target.value : endDate);setSelectingEnd(true)} }} /></label>
        <label>To<input type="date" value={endDate} min={startDate} onChange={e => {if(e.target.value && e.target.value >= startDate) { onChange(startDate,e.target.value);setSelectingEnd(false)}}} /></label>
      </div>
      <button type="button" className="ld-range-done" onClick={() => {setOpen(false);setSelectingEnd(false)}}>Done</button>
    </div>}
  </div>
}
