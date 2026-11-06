import {
  afterNextRender,
  Component,
  computed,
  effect,
  signal,
  ElementRef,
  inject,
  input,
  OnDestroy,
  viewChild,
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { DashboardMetric } from '../../core/api/api.models';
import { ThemeService } from '../../core/theme/theme.service';

Chart.register(...registerables);

type BarChart = Chart<'bar', number[], string>;

/**
 * Chart.js bar chart fed exclusively by metrics that the API marked as
 * available and that carry a numeric value. The dashboard only renders this
 * component when at least two such metrics exist, so no series is fabricated.
 */
@Component({
  selector: 'app-metrics-chart',
  standalone: true,
  template: `
    <div class="chart-frame">
      <canvas #canvas role="img" [attr.aria-label]="ariaLabel()"></canvas>
    </div>
  `,
  styles: [
    `
      :host {
        display: block;
      }

      .chart-frame {
        position: relative;
        width: 100%;
        height: 280px;
      }

      canvas {
        max-width: 100%;
      }
    `,
  ],
})
export class MetricsChart implements OnDestroy {
  readonly metrics = input.required<readonly DashboardMetric[]>();

  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private readonly theme = inject(ThemeService);
  private chart: BarChart | null = null;
  private readonly viewReady = signal(false);

  protected readonly usableMetrics = computed(() =>
    this.metrics().filter(
      (metric): metric is DashboardMetric & { value: number } =>
        metric.available && typeof metric.value === 'number' && Number.isFinite(metric.value),
    ),
  );

  protected readonly ariaLabel = computed(() => {
    const parts = this.usableMetrics().map((metric) => `${metric.label}: ${metric.value}`);
    return parts.length > 0 ? `Bar chart. ${parts.join(', ')}` : 'Bar chart with no available metrics.';
  });

  constructor() {
    // afterNextRender must be called from an injection context; the effect
    // callback is not one, so the view readiness is captured here once.
    afterNextRender(() => this.viewReady.set(true));

    effect(() => {
      // Read both signals so a theme or data change schedules a rebuild.
      const metrics = this.usableMetrics();
      this.theme.mode();
      if (!this.viewReady()) {
        return;
      }
      this.render(metrics);
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
    this.chart = null;
  }

  private render(metrics: readonly (DashboardMetric & { value: number })[]): void {
    if (metrics.length < 2) {
      this.chart?.destroy();
      this.chart = null;
      return;
    }

    const canvas = this.canvas().nativeElement;
    const styles = getComputedStyle(document.documentElement);
    const barColor = styles.getPropertyValue('--bankops-chart-bar').trim() || '#3b5bdb';
    const barColorAlt = styles.getPropertyValue('--bankops-chart-bar-alt').trim() || '#5c7cfa';
    const textColor = styles.getPropertyValue('--bankops-chart-text').trim() || '#343a40';
    const gridColor = styles.getPropertyValue('--bankops-chart-grid').trim() || 'rgba(0, 0, 0, 0.08)';

    const configuration: ChartConfiguration<'bar', number[], string> = {
      type: 'bar',
      data: {
        labels: metrics.map((metric) => metric.label),
        datasets: [
          {
            label: 'Measured value',
            data: metrics.map((metric) => metric.value),
            backgroundColor: metrics.map((_metric, index) =>
              index % 2 === 0 ? barColor : barColorAlt,
            ),
            borderRadius: 6,
            maxBarThickness: 72,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 200 },
        plugins: {
          legend: { display: false },
          tooltip: { enabled: true },
        },
        scales: {
          x: {
            ticks: { color: textColor },
            grid: { display: false },
            border: { color: gridColor },
          },
          y: {
            beginAtZero: true,
            ticks: { color: textColor, precision: 0 },
            grid: { color: gridColor },
            border: { display: false },
          },
        },
      },
    };

    this.chart?.destroy();
    this.chart = new Chart(canvas, configuration);
  }
}
