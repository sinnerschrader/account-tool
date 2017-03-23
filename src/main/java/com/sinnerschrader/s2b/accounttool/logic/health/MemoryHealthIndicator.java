package com.sinnerschrader.s2b.accounttool.logic.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;

/**
 * Memory Health Check Indicator. This indicator submits LOW_MEM status, if the free memory reaches
 * the maximum allocatable memory.
 */
@Component
public class MemoryHealthIndicator extends AbstractHealthIndicator {

	/**
	 * Status for Low Memory
	 */
	private final Status LOW_MEM = new Status("LOW_MEMORY", "The free memory is very low.");

	/**
	 * Constant of required min free memory
	 */
	private final long MIN_FREE_MEMORY = 15 * 1024 * 1024;

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		Runtime runtime = Runtime.getRuntime();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		long maxFreeMemory = runtime.maxMemory() - usedMemory;

		builder.withDetail("total", humanReadableSize(runtime.totalMemory()));
		builder.withDetail("free", humanReadableSize(runtime.freeMemory()));
		builder.withDetail("max", humanReadableSize(runtime.maxMemory()));
		builder.withDetail("used", humanReadableSize(usedMemory));
		if (maxFreeMemory > MIN_FREE_MEMORY) {
			builder.up();
		} else {
			builder.status(LOW_MEM);
		}
	}

	private String humanReadableSize(long size) {
		if (size <= 0) {
			return "0";
		}
		final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.##").format(size / Math.pow(1024, digitGroups))
				+ " "
				+ units[digitGroups];
	}
}
