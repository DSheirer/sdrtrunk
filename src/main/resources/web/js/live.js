// The live WebSocket: one connection per tab, shared by every view.  Opened with a one-use ticket.  When the
// connection drops, it reconnects with a growing delay and subscribes again; each subscription's handler gets a
// 'resubscribed' call so the view can reload what it missed.
import {api, API} from './api.js';

class Live {
  constructor() {
    this.socket = null;
    this.opening = null;
    this.nextSub = 1;
    this.subscriptions = new Map();
    this.retryDelay = 1000;
    this.retryTimer = null;
    this.statusListeners = new Set();
    this.connected = false;
  }

  onStatus(listener) {
    this.statusListeners.add(listener);
    listener(this.connected);
    return () => this.statusListeners.delete(listener);
  }

  setConnected(connected) {
    this.connected = connected;
    this.statusListeners.forEach((listener) => listener(connected));
  }

  open() {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      return Promise.resolve(this.socket);
    }
    if (!this.opening) {
      this.opening = api('POST', '/auth/ws-ticket').then((ticket) => new Promise((resolve, reject) => {
        const scheme = location.protocol === 'https:' ? 'wss://' : 'ws://';
        const socket = new WebSocket(scheme + location.host + API + '/ws?ticket=' +
          encodeURIComponent(ticket.ticket));
        socket.binaryType = 'arraybuffer';
        socket.onopen = () => {
          this.socket = socket;
          this.opening = null;
          this.retryDelay = 1000;
          this.setConnected(true);
          resolve(socket);
        };
        socket.onerror = () => reject(new Error('The live connection failed'));
        socket.onclose = (event) => this.closed(event);
        socket.onmessage = (event) => this.message(event);
      })).catch((error) => {
        this.opening = null;
        this.scheduleReconnect();
        throw error;
      });
    }
    return this.opening;
  }

  // Sends a subscription once per connection.  Returns true if it was sent now.
  send(socket, sub, subscription) {
    if (subscription.socket === socket) {
      return false;
    }
    subscription.socket = socket;
    socket.send(JSON.stringify({op: 'subscribe', sub, topic: subscription.topic, params: subscription.params}));
    return true;
  }

  closed(event) {
    this.socket = null;
    this.opening = null;
    this.setConnected(false);
    // 4001: signed out.  Otherwise reconnect while anything is subscribed.
    if (event.code !== 4001 && this.subscriptions.size > 0) {
      this.scheduleReconnect();
    }
  }

  scheduleReconnect() {
    if (this.retryTimer || this.subscriptions.size === 0) {
      return;
    }
    this.retryTimer = setTimeout(async () => {
      this.retryTimer = null;
      try {
        const socket = await this.open();
        for (const [sub, subscription] of this.subscriptions) {
          if (this.send(socket, sub, subscription) && subscription.handler.resubscribed) {
            subscription.handler.resubscribed();
          }
        }
      } catch (e) {
        // open() scheduled the next attempt
      }
    }, this.retryDelay);
    this.retryDelay = Math.min(this.retryDelay * 2, 30000);
  }

  message(event) {
    if (typeof event.data === 'string') {
      const message = JSON.parse(event.data);
      const subscription = this.subscriptions.get(message.sub);
      if (!subscription) {
        return;
      }
      if (message.op === 'data') {
        subscription.handler.json(message.data, message);
      } else if (message.op === 'error' && subscription.handler.error) {
        subscription.handler.error(message);
      }
    } else {
      const view = new DataView(event.data);
      const subscription = this.subscriptions.get(view.getUint32(4, true));
      if (subscription && subscription.handler.binary) {
        subscription.handler.binary(view, event.data);
      }
    }
  }

  // Subscribes to a topic.  Returns a handle with update(params) and close().  When the connection is down, the
  // subscription is sent when it reconnects.
  async subscribe(topic, params, handler) {
    const sub = this.nextSub++;
    const subscription = {topic, params, handler, socket: null};
    this.subscriptions.set(sub, subscription);
    try {
      this.send(await this.open(), sub, subscription);
    } catch (e) {
      // open() scheduled a reconnect, which sends the subscription
    }
    return {
      update: (newParams) => {
        const subscription = this.subscriptions.get(sub);
        if (subscription) {
          subscription.params = newParams;
          if (this.socket) {
            this.socket.send(JSON.stringify({op: 'update', sub, params: newParams}));
          }
        }
      },
      close: () => {
        if (this.subscriptions.delete(sub) && this.socket && this.socket.readyState === WebSocket.OPEN) {
          this.socket.send(JSON.stringify({op: 'unsubscribe', sub}));
        }
      }
    };
  }

  // Ends every subscription and the connection (sign out)
  closeAll() {
    this.subscriptions.clear();
    clearTimeout(this.retryTimer);
    this.retryTimer = null;
    if (this.socket) {
      this.socket.close(1000);
    }
  }
}

export const live = new Live();
