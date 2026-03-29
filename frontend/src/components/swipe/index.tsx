import {
	type ComponentProps,
	type PointerEvent,
	type ReactNode,
	useRef,
} from 'react';
import { Button } from '@/components/ui/button.tsx';
import { cn } from '@/lib/utils.ts';

export type SwipeActionProps = {
	action: VoidFunction;
	children: ReactNode;
	className?: string;
};

type SwipeActionsProps = {
	main: ReactNode;
	actions?: SwipeActionProps[];
	className?: string;
};

export function SwipeAction({ main, actions, className }: SwipeActionsProps) {
	const actionsRef = useRef<HTMLDivElement>(null);
	const mainRef = useRef<HTMLDivElement>(null);
	const offsetX = useRef(0);
	const startX = useRef(0);
	const startY = useRef(0);
	const dragging = useRef(false);
	const direction = useRef<'h' | 'v' | null>(null);

	if (!actions || actions.length === 0) {
		return <div className={className}>{main}</div>;
	}

	function getMaxSlide() {
		return actionsRef.current?.offsetWidth ?? 0;
	}

	function setTranslate(x: number, animate: boolean) {
		if (!mainRef.current) return;
		mainRef.current.style.transition = animate
			? 'transform 0.3s ease-out'
			: 'none';
		mainRef.current.style.transform = x === 0 ? '' : `translateX(${x}px)`;
	}

	function close() {
		offsetX.current = 0;
		setTranslate(0, true);
	}

	function onPointerDown(e: PointerEvent) {
		dragging.current = true;
		direction.current = null;
		startX.current = e.clientX;
		startY.current = e.clientY;
	}

	function onPointerMove(e: PointerEvent) {
		if (!dragging.current) return;

		const dx = e.clientX - startX.current;
		const dy = e.clientY - startY.current;

		if (!direction.current) {
			if (Math.abs(dx) < 5 && Math.abs(dy) < 5) return;
			direction.current = Math.abs(dx) > Math.abs(dy) ? 'h' : 'v';
		}

		if (direction.current === 'v') return;

		const maxSlide = getMaxSlide();
		const raw = offsetX.current + dx;
		setTranslate(Math.max(-maxSlide, Math.min(0, raw)), false);
	}

	function onPointerUp(e: PointerEvent) {
		if (!dragging.current) return;
		dragging.current = false;

		if (direction.current !== 'h') return;

		const dx = e.clientX - startX.current;
		const maxSlide = getMaxSlide();
		const raw = offsetX.current + dx;

		if (Math.abs(raw) > maxSlide * 0.4) {
			offsetX.current = -maxSlide;
		} else {
			offsetX.current = 0;
		}
		setTranslate(offsetX.current, true);
	}

	return (
		<div className={cn('relative overflow-hidden', className)}>
			<div
				ref={actionsRef}
				className="absolute inset-y-0 right-0 flex items-center"
			>
				{actions.map(({ action, children, className }, index) => (
					<Action
						key={index}
						className={className}
						onClick={action}
						close={close}
					>
						{children}
					</Action>
				))}
			</div>
			<div
				ref={mainRef}
				className="relative z-10 bg-background touch-pan-y"
				onPointerDown={onPointerDown}
				onPointerMove={onPointerMove}
				onPointerUp={onPointerUp}
				onPointerCancel={() => {
					dragging.current = false;
				}}
			>
				{main}
			</div>
		</div>
	);
}

interface SwipeActionActionsProps extends ComponentProps<'button'> {
	close: VoidFunction;
}

const Action = ({
	className,
	children,
	onClick,
	close,
	...props
}: SwipeActionActionsProps) => {
	return (
		<Button
			className={cn(
				'rounded-none rounded-r-md border-0 text-white',
				'z-1 flex aspect-square h-full flex-col items-center justify-center',
				'last:z-0 last:-ml-1.5',
				className,
			)}
			onClick={(e) => {
				onClick?.(e);
				if (!e.defaultPrevented) {
					close();
					(document.activeElement as HTMLElement | null)?.blur();
				}
			}}
			{...props}
		>
			{children}
		</Button>
	);
};
