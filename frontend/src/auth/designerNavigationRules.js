/** Designer tools share the dashboard navigation state, including nested portfolio routes. */
export function isDesignerWorkspacePath(pathname) {
  return pathname === '/designer' || pathname.startsWith('/designer/')
}
