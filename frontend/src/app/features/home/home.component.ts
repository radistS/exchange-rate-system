import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { RouterLink } from '@angular/router';
import { CURRENCY_CODES, SUPPORTED_CURRENCIES } from '../../core/constants/currencies';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatChipsModule, RouterLink],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  readonly currencies = SUPPORTED_CURRENCIES;
  readonly currencyCodes = CURRENCY_CODES;
}
