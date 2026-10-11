import { useState } from "react";
import { Link } from "react-router-dom";
import usePolling from "../hooks/usePolling";
import { unreadCount } from "../api/notifications";

export default function NotificationBell() {
  const [count, setCount] = useState(0);

  usePolling(() => {
    unreadCount().then(setCount).catch(() => {});
  }, 30000, []);

  const label = count > 0 ? `Notifications, ${count} unread` : "Notifications";
  return (
    <Link to="/notifications" aria-label={label} className="relative inline-flex items-center hover:text-brand-600">
      <span aria-hidden="true" className="text-lg">&#128276;</span>
      {count > 0 && (
        <span className="absolute -right-2 -top-2 min-w-[1.1rem] rounded-full bg-red-600 px-1 text-center text-[11px] font-bold leading-[1.1rem] text-white">
          {count > 99 ? "99+" : count}
        </span>
      )}
    </Link>
  );
}