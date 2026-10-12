from database.db import run_query


def convert_to_base(amount, from_currency, base_currency):
    if from_currency == base_currency:
        return float(amount), 1.0

    from_rate = run_query("SELECT rate_to_inr FROM currencies WHERE code = %s", (from_currency,), fetch_one=True)
    base_rate = run_query("SELECT rate_to_inr FROM currencies WHERE code = %s", (base_currency,), fetch_one=True)

    if not from_rate or not base_rate:
        return float(amount), 1.0  # fallback: no conversion possible, treat as 1:1

    # Convert: original -> INR -> base_currency
    in_inr = float(amount) * float(from_rate["rate_to_inr"])
    converted = in_inr / float(base_rate["rate_to_inr"])
    exchange_rate = converted / float(amount) if float(amount) != 0 else 1.0

    return round(converted, 2), round(exchange_rate, 6)