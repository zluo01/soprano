import { Progress } from '@/components/ui/progress.tsx';
import { format } from '@/lib/utils.ts';

interface IProgressProps {
  elapsed: number;
  duration: number;
}

export default function ProgressPreview({ elapsed, duration }: IProgressProps) {
  return (
    <div className="flex w-full flex-row items-center justify-between gap-2">
      <p className="cursor-default text-center text-sm"> {format(elapsed)}</p>
      <Progress
        value={duration === 0 ? 0 : (elapsed * 100) / duration}
        className="w-full"
      />
      <p className="cursor-default text-center text-sm"> {format(duration)}</p>
    </div>
  );
}
