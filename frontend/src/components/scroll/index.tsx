import { ScrollArea } from '@/components/ui/scroll-area.tsx';
import { ReactNode, useEffect, useRef } from 'react';
import { useLocation } from 'react-router';

interface IScrollProps {
  className: string;
  children: ReactNode;
}

/**
 * Modified from https://github.com/shadcn-ui/ui/discussions/3404#discussioncomment-11419441
 */
export default function ScrollContainer({ className, children }: IScrollProps) {
  const location = useLocation();

  const scrollAreaRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const savedPosition = localStorage.getItem(
      `${location.pathname}-scrollPosition`,
    );
    if (savedPosition && scrollAreaRef.current) {
      const element = scrollAreaRef.current?.querySelector(
        '[data-radix-scroll-area-viewport]',
      );
      if (element) {
        element.scrollTop = parseInt(savedPosition, 10);
      }
    }
  }, [location.pathname]);

  const handleScroll = () => {
    const chatArea = scrollAreaRef.current?.querySelector(
      '[data-radix-scroll-area-viewport]',
    );
    const scrollTop = chatArea?.scrollTop || 0;
    localStorage.setItem(
      `${location.pathname}-scrollPosition`,
      scrollTop.toString(),
    );
  };

  return (
    <ScrollArea
      ref={scrollAreaRef}
      onScrollCapture={handleScroll}
      className={className}
    >
      {children}
    </ScrollArea>
  );
}
