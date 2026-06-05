declare module "sockjs-client" {
  export default class SockJS extends WebSocket {
    constructor(url: string, protocols?: string | string[], options?: unknown);
  }
}

