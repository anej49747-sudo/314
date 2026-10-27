import { Alert, Button, Spin } from 'antd'
import { ArrowDown, MessageSquare, Send } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { currentSession } from '../api/authentication.js'
import { getDesignerProfile } from '../api/designerProfile.js'
import { contactDesigner, getConversationMessages, listConversations, sendConversationMessage } from '../api/messaging.js'
import { initials, MAX_MESSAGE_LENGTH, messageErrorMessage, validDesignerId, validMessageBody } from '../messaging/messageRules.js'
import '../messaging/messages.css'

function timeLabel(time) {
  if (!time) return ''
  const date = new Date(time)
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('en-SG', {
    dateStyle: 'medium', timeStyle: 'short',
  }).format(date)
}

export default function MessagesPage({ role }) {
  const navigate = useNavigate()
  const location = useLocation()
  const isCustomer = role === 'customer'
  const [account, setAccount] = useState(null)
  const [threads, setThreads] = useState([])
  const [draft, setDraft] = useState(null)
  const [selected, setSelected] = useState(null)
  const [messages, setMessages] = useState([])
  const [hasMore, setHasMore] = useState(false)
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(true)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [loadingOlder, setLoadingOlder] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [messageError, setMessageError] = useState('')
  const [refresh, setRefresh] = useState(0)
  const scrollRef = useRef(null)
  const skipAutoScroll = useRef(false)

  useEffect(() => {
    let active = true
    const params = new URLSearchParams(location.search)
    const targetId = isCustomer ? params.get('designerId') : null
    async function initialise() {
      setLoading(true)
      setError('')
      try {
        const session = await currentSession()
        if (!active) return
        const expectedRole = isCustomer ? 'CUSTOMER' : 'INTERIOR_DESIGNER'
        if (session.role !== expectedRole) {
          navigate(session.role === 'CUSTOMER' ? '/customer' : session.role === 'INTERIOR_DESIGNER'
            ? '/designer' : '/', { replace: true })
          return
        }
        if (!isCustomer && session.passwordChangeRequired) {
          navigate('/designer/change-password', { replace: true })
          return
        }
        const list = await listConversations()
        if (!active) return
        setAccount(session)
        setThreads(list)
        if (targetId && validDesignerId(targetId)) {
          const existing = list.find((thread) => thread.counterpartId === Number(targetId))
          if (existing) {
            setDraft(null)
            setSelected(existing.id)
          } else {
            try {
              const profile = await getDesignerProfile(targetId)
              if (!active) return
              setDraft({ counterpartId: profile.id, counterpartName: profile.name })
              setSelected('draft')
            } catch (failure) {
              if (active) setError(messageErrorMessage(failure))
              setDraft(null)
              setSelected(list[0]?.id ?? null)
            }
          }
        } else if (targetId) {
          setError('Invalid designer link.')
          setDraft(null)
          setSelected(list[0]?.id ?? null)
        } else {
          setDraft(null)
          setSelected((previous) => list.some((thread) => thread.id === previous) ? previous : list[0]?.id ?? null)
        }
      } catch (failure) {
        if (!active) return
        if (failure.response?.status === 401) navigate('/login', { replace: true })
        else setError(messageErrorMessage(failure))
      } finally {
        if (active) setLoading(false)
      }
    }
    initialise()
    return () => { active = false }
  }, [isCustomer, location.search, navigate, refresh])

  useEffect(() => {
    let active = true
    setMessageError('')
    setMessages([])
    setHasMore(false)
    if (!selected || selected === 'draft') { setLoadingMessages(false); return () => { active = false } }
    setLoadingMessages(true)
    getConversationMessages(selected).then((page) => {
      if (active) { setMessages(page.messages); setHasMore(page.hasMore) }
    }).catch((failure) => {
      if (active) setMessageError(messageErrorMessage(failure))
    }).finally(() => { if (active) setLoadingMessages(false) })
    return () => { active = false }
  }, [selected, refresh])

  useEffect(() => {
    if (scrollRef.current && !loadingMessages && !loadingOlder) {
      if (skipAutoScroll.current) { skipAutoScroll.current = false; return }
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight
    }
  }, [selected, loadingMessages, messages.length, loadingOlder])

  const currentThread = threads.find((thread) => thread.id === selected)
  const counterpart = selected === 'draft' ? draft : currentThread
  const conversationList = draft
    ? [{ ...draft, id: 'draft', lastMessagePreview: 'Start a conversation' }, ...threads]
    : threads

  async function loadOlder() {
    if (loadingOlder || !hasMore || !messages.length || selected === 'draft') return
    setLoadingOlder(true)
    skipAutoScroll.current = true
    setMessageError('')
    try {
      const page = await getConversationMessages(selected, messages[0].id)
      setMessages((current) => [...page.messages, ...current])
      setHasMore(page.hasMore)
    } catch (failure) { skipAutoScroll.current = false; setMessageError(messageErrorMessage(failure)) }
    finally { setLoadingOlder(false) }
  }

  async function send(event) {
    event.preventDefault()
    if (sending || !counterpart || !validMessageBody(text)) return
    setSending(true)
    setMessageError('')
    try {
      const created = selected === 'draft'
        ? await contactDesigner(draft.counterpartId, text.trim())
        : await sendConversationMessage(selected, text.trim())
      setText('')
      if (selected === 'draft') {
        setDraft(null)
        setSelected(created.conversationId)
      } else {
        setMessages((current) => [...current, created])
      }
      try {
        const list = await listConversations()
        setThreads(list)
      } catch {
        setMessageError('Your message was sent, but the conversation list could not refresh.')
      }
    } catch (failure) { setMessageError(messageErrorMessage(failure)) }
    finally { setSending(false) }
  }

  return <section className="ld-messages" aria-labelledby="ld-messages-title">
    <header className="ld-messages-page-head">
      <div><p className="ui-eyebrow">{isCustomer ? 'My space' : 'Designer workspace'}</p>
        <h1 id="ld-messages-title">{isCustomer ? 'My messages' : 'Customer messages'}</h1>
        <p>{isCustomer ? 'Your private conversations with interior designers.'
          : 'Read and reply to messages sent by customers.'}</p></div>
      <Button onClick={() => setRefresh((value) => value + 1)} disabled={loading || sending}>
        Refresh messages
      </Button>
    </header>
    {error && <Alert type="error" role="alert" showIcon message={error} className="ld-messages-alert" />}
    {loading && <div className="loading-state" role="status"><Spin /> Loading conversations…</div>}
    {!loading && account && <div className="ld-messages-layout">
      <aside className="ld-messages-side" aria-label="Conversation list">
        <div className="ld-messages-side-title"><h2>Conversations</h2>
          <p>{isCustomer ? 'Select a designer to view your messages.'
            : 'Select a customer to view your messages.'}</p></div>
        <div className="ld-messages-thread-list">
          {conversationList.length === 0 && <p className="ld-messages-no-threads">
            {isCustomer ? 'No conversations yet. Visit a designer profile to send your first message.'
              : 'No messages yet. Customer conversations will appear here.'}</p>}
          {conversationList.map((thread) => <button type="button" key={thread.id}
            className={`ld-messages-thread ${selected === thread.id ? 'is-selected' : ''}`}
            aria-current={selected === thread.id ? 'true' : undefined}
            onClick={() => { setSelected(thread.id); setText('') }}>
            <span className="ld-messages-avatar" aria-hidden="true">{initials(thread.counterpartName)}</span>
            <span className="ld-messages-thread-copy"><strong>{thread.counterpartName}</strong>
              <span>{thread.lastMessagePreview || 'Start a conversation'}</span></span>
          </button>)}
        </div>
      </aside>
      <div className="ld-messages-conversation">
        {counterpart ? <>
          <header className="ld-messages-conversation-head">
            <h2>{counterpart.counterpartName}</h2>
            <p>{isCustomer ? 'Interior designer' : 'Customer'} · Only the participants can view this conversation</p>
          </header>
          <div className="ld-messages-scroll" ref={scrollRef} aria-live="polite">
            {loadingMessages ? <div className="loading-state"><Spin /> Loading messages…</div> : <>
              {hasMore && <Button icon={<ArrowDown size={14} />} size="small"
                onClick={loadOlder} loading={loadingOlder} className="ld-messages-older">
                Load earlier messages
              </Button>}
              {messages.length === 0 && <div className="ld-messages-empty">
                <span className="ld-messages-empty-icon"><MessageSquare size={32} /></span>
                <h3>Start the conversation</h3>
                <p>{isCustomer ? 'Introduce your renovation plans. Your first message will create the conversation.'
                  : 'Customer messages will appear here.'}</p></div>}
              {messages.map((message) => <div key={message.id}
                className={`ld-messages-bubble-wrap ${message.senderId === account.id ? 'is-mine' : ''}`}>
                <div className="ld-messages-bubble">{message.body}</div>
                <time className="ld-messages-time" dateTime={message.createdAt}>{timeLabel(message.createdAt)}</time>
              </div>)}
            </>}
          </div>
          <form className="ld-messages-composer" onSubmit={send}>
            {messageError && <Alert type="error" role="alert" showIcon message={messageError}
              className="ld-messages-alert" />}
            <label htmlFor="ld-message-input">{isCustomer ? 'Write a message' : 'Reply to customer'}</label>
            <div className="ld-messages-compose-row">
              <textarea id="ld-message-input" value={text} maxLength={MAX_MESSAGE_LENGTH}
                rows={3} placeholder="Write your message here…"
                onChange={(event) => setText(event.target.value)} disabled={sending}/>
              <Button htmlType="submit" type="primary" icon={<Send size={16} />}
                loading={sending} disabled={!validMessageBody(text) || loadingMessages}>Send</Button>
            </div>
            <div className="ld-messages-composer-notes"><span>Do not share passwords or payment details in messages.</span>
              <span>{text.length} / {MAX_MESSAGE_LENGTH}</span></div>
          </form>
        </> : <div className="ld-messages-empty ld-messages-empty-all">
          <span className="ld-messages-empty-icon"><MessageSquare size={32} /></span>
          <h3>No conversation selected</h3>
          <p>{isCustomer ? 'Visit a designer profile to start a private conversation.'
            : 'Messages from customers will appear here.'}</p>
        </div>}
      </div>
    </div>}
  </section>
}
