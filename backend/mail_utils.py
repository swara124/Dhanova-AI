import smtplib
import os
from email.mime.text import MIMEText
from dotenv import load_dotenv

load_dotenv()

SMTP_EMAIL = os.getenv("SMTP_EMAIL")
SMTP_APP_PASSWORD = os.getenv("SMTP_APP_PASSWORD")


def send_family_invitation_email(to_email, family_name, inviter_name, is_registered_user):
    if not SMTP_EMAIL or not SMTP_APP_PASSWORD:
        print("WARNING: SMTP not configured, skipping email send")
        return False

    subject = f"{inviter_name} invited you to join {family_name} on Dhanova AI"

    if is_registered_user:
        body = (
            f"Hi,\n\n{inviter_name} has invited you to join their family '{family_name}' "
            f"on Dhanova AI.\n\nOpen the Dhanova AI app and check your Invitations screen "
            f"to Accept or Reject this request.\n\n— Dhanova AI"
        )
    else:
        body = (
            f"Hi,\n\n{inviter_name} has invited you to join their family '{family_name}' "
            f"on Dhanova AI, a family finance management app.\n\n"
            f"You don't have a Dhanova AI account yet — download the app and register "
            f"using this exact email address ({to_email}). Once registered, you'll see "
            f"this invitation waiting for you to Accept.\n\n— Dhanova AI"
        )

    msg = MIMEText(body)
    msg["Subject"] = subject
    msg["From"] = SMTP_EMAIL
    msg["To"] = to_email

    try:
        with smtplib.SMTP("smtp.gmail.com", 587) as server:
            server.starttls()
            server.login(SMTP_EMAIL, SMTP_APP_PASSWORD)
            server.sendmail(SMTP_EMAIL, [to_email], msg.as_string())
        return True
    except Exception as e:
        print(f"Email send failed: {e}")
        return False