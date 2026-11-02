import React, { useEffect, useMemo, useState } from 'react'
import { api } from './api'
import { Empty, ErrorPanel, Status } from './components'
import { CheckCircle2, ClipboardCheck, Clock3, Files, History, MailWarning, RefreshCw, Search, X } from 'lucide-react'

const TYPES = { STUDY: 'Study Certificate', BONAFIDE: 'Bonafide Certificate', LOR: 'Letter of Recommendation', DUPLICATE_TC: 'Duplicate TC', CONDUCT: 'Conduct Certificate' }
const STATES = ['PENDING_HOD', 'PENDING_PRINCIPAL', 'APPROVED', 'REJECTED', 'CANCELLED', 'COLLECTED', 'EXPIRED']
const date = v => v ? new Date(v).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short', timeZone: 'Asia/Kolkata' }) : '—'

export default function CertificateQueue({ role }) {
  const base = `/${role}/certificates`
  const [tab, setTab] = useState('all')
  const [items, setItems] = useState([])
  const [stats, setStats] = useState({})
  const [failed, setFailed] = useState([])
  const [detail, setDetail] = useState(null)
  const [choice, setChoice] = useState(null)
  const [remarks, setRemarks] = useState('')
  const [identity, setIdentity] = useState(false)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const [type, setType] = useState('')
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)

  async function refresh() {
    setError(''); setLoading(true)
    try {
      const [requests, analytics] = await Promise.all([api(base), api(`${base}/analytics`)])
      setItems(Array.isArray(requests) ? requests : []); setStats(analytics || {})
      if (role === 'admin') setFailed(await api('/admin/certificates/failed-notifications'))
    } catch (err) { setError(err.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { refresh() }, [role])
  const filtered = useMemo(() => items.filter(item =>
    (role !== 'admin' || tab === 'all' || tab === 'failed' || (tab === 'pending' && item.status === 'APPROVED') || (tab === 'collected' && item.status === 'COLLECTED') || (tab === 'closed' && ['REJECTED', 'CANCELLED', 'EXPIRED'].includes(item.status))) &&
    (!search || [item.rollNumber, item.studentName, String(item.id)].some(value => String(value || '').toLowerCase().includes(search.toLowerCase()))) &&
    (!status || item.status === status) && (!type || item.certificateType === type)
  ), [items, role, tab, search, status, type])

  async function run(action) {
    setBusy(true); setError(''); setNotice('')
    try { await action(); setNotice('Request updated successfully.'); setChoice(null); setRemarks(''); setIdentity(false); await refresh() }
    catch (err) { setError(err.message) } finally { setBusy(false) }
  }
  async function open(id) { setError(''); try { setDetail(await api(`${base}/${id}`)) } catch (err) { setError(err.message) } }
  async function confirm() {
    if (!choice) return
    const { item, action } = choice
    if (['reject', 'cancel'].includes(action) && !remarks.trim()) return
    if (action === 'collect' && !identity) return
    const path = action === 'collect' ? `${base}/${item.id}/collect` : action === 'cancel' ? `${base}/${item.id}/cancel` : `${base}/${item.id}/${action}`
    await run(() => api(path, { method: action === 'collect' ? 'POST' : 'PATCH', body: action === 'collect' ? { identityVerified: identity, remarks } : { remarks } }))
  }
  const actButtons = item => {
    if (role === 'staff' && item.status === 'PENDING_HOD') return <div className="campus-service-actions"><button className="btn btn-primary" onClick={() => setChoice({ item, action: 'approve' })}>Approve</button><button className="btn btn-muted" onClick={() => setChoice({ item, action: 'reject' })}>Reject</button></div>
    if (role === 'principal' && item.status === 'PENDING_PRINCIPAL') return <div className="campus-service-actions"><button className="btn btn-primary" onClick={() => setChoice({ item, action: 'approve' })}>Approve</button><button className="btn btn-muted" onClick={() => setChoice({ item, action: 'reject' })}>Reject</button></div>
    if (role === 'admin') return <div className="campus-service-actions">{item.status === 'APPROVED' && <button className="btn btn-primary" onClick={() => setChoice({ item, action: 'collect' })}>Record collection</button>}{!['COLLECTED', 'REJECTED', 'CANCELLED', 'EXPIRED'].includes(item.status) && <button className="btn btn-muted" onClick={() => setChoice({ item, action: 'cancel' })}>Cancel</button>}</div>
    return null
  }
  return <section className="campus-service-section space-y-5" aria-label="Certificate Requests">
    <div className="card p-5 sm:p-6">
      <div className="mb-5 flex flex-wrap items-start justify-between gap-3"><div><p className="campus-eyebrow mb-1">Certificate Services / {role === 'staff' ? 'HOD' : role === 'principal' ? 'Principal' : 'Administration'}</p><h2 className="campus-service-heading">Certificate Requests</h2><p className="tiny mt-1">Independent of the 17-department No Dues approval workflow</p></div><button className="btn btn-muted" onClick={refresh} disabled={busy || loading}><RefreshCw size={15} className={loading ? 'animate-spin' : ''} /> Refresh</button></div>
      <ErrorPanel error={error} />
      {notice && <div className="campus-service-note mb-4" role="status">{notice}</div>}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4 mb-6">
        {Object.entries(stats).filter(([key]) => ['total', ...STATES].includes(key)).map(([key, value]) => <div className="campus-service-metric" key={key}><p className="tiny">{key.replaceAll('_', ' ')}</p><strong>{Number(value || 0).toLocaleString('en-IN')}</strong></div>)}
      </div>
      <div className="input-grid">
        <label><span className="label">Find request</span><div className="relative"><Search className="absolute left-3 top-3.5 text-slate-400" size={16} /><input className="field !pl-10" value={search} placeholder="Roll number, student, request ID" onChange={e => setSearch(e.target.value)} /></div></label>
        <label><span className="label">Status</span><select className="field" value={status} onChange={e => setStatus(e.target.value)}><option value="">All statuses</option>{STATES.map(value => <option key={value} value={value}>{value.replaceAll('_', ' ')}</option>)}</select></label>
        <label><span className="label">Certificate type</span><select className="field" value={type} onChange={e => setType(e.target.value)}><option value="">All certificate types</option>{Object.entries(TYPES).map(([key, label]) => <option value={key} key={key}>{label}</option>)}</select></label>
      </div>
    </div>
    {role === 'admin' && <nav className="campus-service-tabs" aria-label="Certificate management views">{[
      ['all', 'All Requests', Files], ['pending', 'Pending Collections', Clock3], ['collected', 'Collected History', CheckCircle2], ['closed', 'Cancelled / Rejected', History], ['failed', 'Failed Notifications', MailWarning],
    ].map(([key, label, Icon]) => <button key={key} className={`btn btn-muted ${tab === key ? 'is-active' : ''}`} onClick={() => setTab(key)} aria-current={tab === key ? 'page' : undefined}><Icon size={15} />{label}</button>)}</nav>}

    {(role !== 'admin' || tab !== 'failed') && <div className="card table-wrap"><table><thead><tr>{['Request', 'Student', 'Certificate', 'Branch HOD', 'Purpose', 'Status', 'Submitted', 'Actions'].map(label => <th key={label}>{label}</th>)}</tr></thead><tbody>
      {filtered.map(item => <tr key={item.id}><td className="font-semibold">#{item.id}</td><td><b className="font-semibold text-slate-800">{item.studentName}</b><p className="tiny">{item.rollNumber}</p></td><td>{TYPES[item.certificateType]}</td><td>{item.branch}</td><td className="max-w-[220px] break-words">{item.purpose}</td><td><Status value={item.status} /></td><td className="whitespace-nowrap">{date(item.submittedAt)}</td><td><div className="campus-service-actions"><button className="btn btn-muted" onClick={() => open(item.id)}>History</button>{actButtons(item)}</div></td></tr>)}
    </tbody></table>{!filtered.length && !loading && <Empty message="No matching certificate requests." />}{loading && <p className="tiny p-5">Loading certificate requests…</p>}</div>}

    <div className="grid gap-4 md:grid-cols-2">{[['Requests by certificate type', stats.byType], ['Requests by routing branch', stats.byBranch]].map(([label, values]) => <div className="card p-5" key={label}><h3 className="campus-service-subheading mb-3">{label}</h3>{Object.entries(values || {}).map(([key, value]) => <div key={key} className="campus-service-row flex justify-between gap-4 text-sm"><span className="text-slate-600">{TYPES[key] || key}</span><b className="tabular-nums">{value}</b></div>)}{!Object.keys(values || {}).length && <p className="tiny">No data available.</p>}</div>)}</div>
    <p className="tiny">Average HOD processing: {Math.round(stats.avgHodMinutes || 0)} min · Average Principal processing: {Math.round(stats.avgPrincipalMinutes || 0)} min{role === 'admin' ? ` · Failed notifications: ${stats.failedNotifications || 0}` : ''}</p>

    {role === 'admin' && (tab === 'failed' || tab === 'all') && <div className="card p-5"><h3 className="campus-service-subheading mb-3">Failed email notifications</h3>{failed.length ? failed.map(item => <div key={item.id} className="campus-service-row flex flex-wrap items-center justify-between gap-3"><div><p className="text-sm font-semibold">Request #{item.requestId} · {item.kind}</p><p className="tiny mt-1">{item.recipient} · {item.lastError}</p></div><button className="btn btn-muted" disabled={busy} onClick={() => run(() => api(`/admin/certificates/${item.requestId}/resend?kind=${encodeURIComponent(item.kind)}`, { method: 'POST' }))}>Resend notification</button></div>) : <p className="tiny">No failed notifications.</p>}</div>}

    {detail && <div className="campus-service-dialog" role="presentation"><div className="campus-service-dialog-inner" role="dialog" aria-modal="true" aria-label="Certificate history"><div className="flex items-start justify-between gap-3 mb-5"><div><h3 className="campus-service-heading">Request #{detail.id}</h3><p className="tiny mt-1">{detail.studentName} · {detail.rollNumber} · {TYPES[detail.certificateType]}</p></div><button type="button" className="btn btn-muted" onClick={() => setDetail(null)} aria-label="Close history"><X size={17} /></button></div><div className="rounded-md border border-slate-200 px-4">{(detail.history || []).map(event => <div className="campus-service-row" key={event.id}><p className="text-sm font-semibold">{event.action.replaceAll('_', ' ')}</p><p className="tiny mt-1">{date(event.occurredAt)} · {event.actorRole}</p>{event.remarks && <p className="text-sm text-slate-600 mt-1">{event.remarks}</p>}</div>)}</div></div></div>}
    {choice && <div className="campus-service-dialog" role="presentation"><div className="campus-service-dialog-inner" role="dialog" aria-modal="true" aria-label={`Confirm ${choice.action}`}><div className="mb-4 flex items-start justify-between gap-3"><div><p className="campus-eyebrow mb-1">Certificate request #{choice.item.id}</p><h3 className="campus-service-heading">Confirm {choice.action}</h3><p className="tiny mt-1">{choice.item.studentName} · {TYPES[choice.item.certificateType]}</p></div><button type="button" className="btn btn-muted" disabled={busy} onClick={() => setChoice(null)}><X size={16} /></button></div>{choice.action === 'collect' && <label className="campus-service-note mb-4 flex items-start gap-3"><input type="checkbox" className="mt-1" checked={identity} onChange={e => setIdentity(e.target.checked)} /><span>I've physically verified the student and am handing over exactly one certificate.</span></label>}<label className="block"><span className="label">{['reject', 'cancel'].includes(choice.action) ? 'Reason (required)' : 'Remarks (optional)'}</span><textarea className="field" rows={4} value={remarks} onChange={e => setRemarks(e.target.value)} /></label><div className="mt-5 flex justify-end gap-2 border-t border-slate-100 pt-4"><button type="button" className="btn btn-muted" disabled={busy} onClick={() => setChoice(null)}>Back</button><button type="button" className={choice.action === 'reject' || choice.action === 'cancel' ? 'btn btn-danger' : 'btn btn-primary'} disabled={busy || (['reject', 'cancel'].includes(choice.action) && !remarks.trim()) || (choice.action === 'collect' && !identity)} onClick={confirm}>{busy ? 'Saving…' : 'Confirm decision'}</button></div></div></div>}
  </section>
}
