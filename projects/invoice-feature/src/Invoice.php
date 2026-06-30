<?php
/**
 * Invoice business object (TASK-101)
 * Built according to the PBMP context. PSR-12 + no debug code.
 */

namespace App\Billing;

final class Invoice
{
    private array $lineItems = [];
    private float $taxRate;

    public function __construct(
        private int $customerId,
        float $taxRate = 0.18
    ) {
        $this->taxRate = $taxRate;
    }

    public function addLineItem(string $name, float $price, int $qty): void
    {
        $this->lineItems[] = [
            'name' => $name,
            'price' => $price,
            'qty' => $qty,
            'amount' => $price * $qty,
        ];
    }

    public function subtotal(): float
    {
        return array_sum(array_column($this->lineItems, 'amount'));
    }

    public function tax(): float
    {
        return round($this->subtotal() * $this->taxRate, 2);
    }

    public function total(): float
    {
        return round($this->subtotal() + $this->tax(), 2);
    }
}
