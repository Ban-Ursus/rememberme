/*
 * Copyright (C) 2013-2020 Federico Iosue (federico@iosue.it)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.feio.android.rememberme;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.pixplicity.easyprefs.library.Prefs;
import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import it.feio.android.rememberme.async.UpdateWidgetsTask;
import it.feio.android.rememberme.async.bus.PasswordRemovedEvent;
import it.feio.android.rememberme.async.bus.SwitchFragmentEvent;
import it.feio.android.rememberme.async.notes.NoteProcessorDelete;
import it.feio.android.rememberme.databinding.ActivityMainBinding;
import it.feio.android.rememberme.db.DbHelper;
import it.feio.android.rememberme.helpers.LogDelegate;
import it.feio.android.rememberme.helpers.NotesHelper;
import it.feio.android.rememberme.intro.IntroActivity;
import it.feio.android.rememberme.models.Attachment;
import it.feio.android.rememberme.models.Category;
import it.feio.android.rememberme.models.Note;
import it.feio.android.rememberme.models.ONStyle;
import it.feio.android.rememberme.utils.FileProviderHelper;
import it.feio.android.rememberme.utils.PasswordHelper;
import it.feio.android.rememberme.utils.SystemHelper;
import it.feio.android.pixlui.links.UrlCompleter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import it.feio.android.rememberme.utils.ConstantsBase;
import lombok.Getter;
import lombok.Setter;


import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

public class MainActivity extends BaseActivity implements
    SharedPreferences.OnSharedPreferenceChangeListener {

  private boolean isPasswordAccepted = false;
  public final static String FRAGMENT_DRAWER_TAG = "fragment_drawer";
  public final static String FRAGMENT_LIST_TAG = "fragment_list";
  public final static String FRAGMENT_DETAIL_TAG = "fragment_detail";
  public final static String FRAGMENT_SKETCH_TAG = "fragment_sketch";
  @Getter @Setter
  private Uri sketchUri;
  boolean prefsChanged = false;
  private FragmentManager mFragmentManager;

  ActivityMainBinding binding;

  String superTitle = "";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTheme(R.style.OmniNotesTheme_ApiSpec);

    binding = ActivityMainBinding.inflate(getLayoutInflater());
    View view = binding.getRoot();
    setContentView(view);

    EventBus.getDefault().register(this);
    Prefs.getPreferences().registerOnSharedPreferenceChangeListener(this);

    initUI();

    if (IntroActivity.mustRun()) {
      startActivity(new Intent(getApplicationContext(), IntroActivity.class));
    }


  }

  @Override
  protected void onResume() {
    super.onResume();
    if (isPasswordAccepted) {
      init();
    } else {
      checkPassword();
    }
  }


  @Override
  protected void onStop() {
    super.onStop();
    EventBus.getDefault().unregister(this);
  }


  private void initUI() {
    setSupportActionBar(binding.toolbar.toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeButtonEnabled(true);
  }


  /**
   * This method starts the bootstrap chain.
   */
  private void checkPassword() {
    if (Prefs.getString(ConstantsBase.PREF_PASSWORD, null) != null
        && Prefs.getBoolean("settings_password_access", false)) {
      PasswordHelper.requestPassword(this, passwordConfirmed -> {
        switch (passwordConfirmed) {
          case SUCCEED:
            init();
            break;
          case FAIL:
            finish();
            break;
          case RESTORE:
            PasswordHelper.resetPassword(this);
        }
      });
    } else {
      init();
    }
  }


  public void onEvent(PasswordRemovedEvent passwordRemovedEvent) {
    showMessage(R.string.password_successfully_removed, ONStyle.ALERT);
    init();
  }


  private void init() {
    isPasswordAccepted = true;

    getFragmentManagerInstance();

    NavigationDrawerFragment mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManagerInstance()
        .findFragmentById(R.id.navigation_drawer);
    if (mNavigationDrawerFragment == null) {
      FragmentTransaction fragmentTransaction = getFragmentManagerInstance().beginTransaction();
      fragmentTransaction.replace(R.id.navigation_drawer, new NavigationDrawerFragment(),
          FRAGMENT_DRAWER_TAG).commit();
    }

    if (getFragmentManagerInstance().findFragmentByTag(FRAGMENT_LIST_TAG) == null) {
      FragmentTransaction fragmentTransaction = getFragmentManagerInstance().beginTransaction();
      fragmentTransaction.add(R.id.fragment_container, new ListFragment(), FRAGMENT_LIST_TAG)
          .commit();
    }

    handleIntents();
  }

  private FragmentManager getFragmentManagerInstance() {
    if (mFragmentManager == null) {
      mFragmentManager = getSupportFragmentManager();
    }
    return mFragmentManager;
  }

  @Override
  protected void onNewIntent(Intent intent) {
    if (intent.getAction() == null) {
      intent.setAction(ConstantsBase.ACTION_START_APP);
    }
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntents();
    LogDelegate.d("onNewIntent");
  }


  public MenuItem getSearchMenuItem() {
    Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
    if (f != null) {
      return ((ListFragment) f).getSearchMenuItem();
    } else {
      return null;
    }
  }


  public void editTag(Category tag) {
    Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
    if (f != null) {
      ((ListFragment) f).editCategory(tag);
    }
  }

  public void initNotesList(Intent intent) {
    if (intent != null) {
      Fragment searchTagFragment = startSearchView();
      new Handler(getMainLooper()).post(() -> ((ListFragment) searchTagFragment).initNotesList(intent));
    }
  }

  public Fragment startSearchView() {
    FragmentTransaction transaction = getFragmentManagerInstance().beginTransaction();
    animateTransition(transaction, TRANSITION_HORIZONTAL);
    ListFragment mListFragment = new ListFragment();
    transaction.replace(R.id.fragment_container, mListFragment, FRAGMENT_LIST_TAG).addToBackStack
        (FRAGMENT_DETAIL_TAG).commit();
    Bundle args = new Bundle();
    args.putBoolean("setSearchFocus", true);
    mListFragment.setArguments(args);
    return mListFragment;
  }


  public void commitPending() {
    Fragment f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
    if (f != null) {
      ((ListFragment) f).commitPending();
    }
  }


  /**
   * Checks if allocated fragment is of the required type and then returns it or returns null
   */
  private Fragment checkFragmentInstance(int id, Object instanceClass) {
    Fragment result = null;
    Fragment fragment = getFragmentManagerInstance().findFragmentById(id);
    if (fragment != null && instanceClass.equals(fragment.getClass())) {
      result = fragment;
    }
    return result;
  }

  @Override
  public void onBackPressed() {

    // SketchFragment
    Fragment f = checkFragmentInstance(R.id.fragment_container, SketchFragment.class);
    if (f != null) {
      ((SketchFragment) f).save();

      // Removes forced portrait orientation for this fragment
      setRequestedOrientation(
          ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

      getFragmentManagerInstance().popBackStack();
      return;
    }

    // DetailFragment
    f = checkFragmentInstance(R.id.fragment_container, DetailFragment.class);
    if (f != null) {
      ((DetailFragment) f).goBack = true;
      ((DetailFragment) f).saveAndExit((DetailFragment) f);
      return;
    }

    // ListFragment
    f = checkFragmentInstance(R.id.fragment_container, ListFragment.class);
    if (f != null) {
      // Before exiting from app the navigation drawer is opened
      if (Prefs.getBoolean("settings_navdrawer_on_exit", false) && getDrawerLayout() != null &&
          !getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
        getDrawerLayout().openDrawer(GravityCompat.START);
      } else if (!Prefs.getBoolean("settings_navdrawer_on_exit", false) && getDrawerLayout() != null
          &&
          getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
        getDrawerLayout().closeDrawer(GravityCompat.START);
      } else {
        if (!((ListFragment) f).closeFab()) {
          isPasswordAccepted = false;
          super.onBackPressed();
        }
      }
      return;
    }
    super.onBackPressed();
  }


  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putString("navigationTmp", navigationTmp);
  }


  @Override
  protected void onPause() {
    super.onPause();
    Crouton.cancelAllCroutons();
  }


  public DrawerLayout getDrawerLayout() {
    return binding.drawerLayout;
  }


  public ActionBarDrawerToggle getDrawerToggle() {
    if (getFragmentManagerInstance().findFragmentById(R.id.navigation_drawer) != null) {
      return ((NavigationDrawerFragment) getFragmentManagerInstance().findFragmentById(
          R.id.navigation_drawer)).mDrawerToggle;
    } else {
      return null;
    }
  }


  /**
   * Finishes multiselection mode started by ListFragment
   */
  public void finishActionMode() {
    ListFragment fragment = (ListFragment) getFragmentManagerInstance()
        .findFragmentByTag(FRAGMENT_LIST_TAG);
    if (fragment != null) {
      fragment.finishActionMode();
    }
  }


  Toolbar getToolbar() {
    return binding.toolbar.toolbar;
  }


  private void handleIntents() {
    Intent i = getIntent();

    if (i.getAction() == null) {
      return;
    }

    if (ConstantsBase.ACTION_RESTART_APP.equals(i.getAction())) {
      SystemHelper.restartApp(getApplicationContext(), MainActivity.class);
    }

    if (receivedIntent(i)) {
      Note note = i.getParcelableExtra(ConstantsBase.INTENT_NOTE);
      if (note == null) {
        note = DbHelper.getInstance().getNote(i.getIntExtra(ConstantsBase.INTENT_KEY, 0));
      }
      // Checks if the same note is already opened to avoid to open again
      if (note != null && noteAlreadyOpened(note)) {
        return;
      }
      // Empty note instantiation
      if (note == null) {
        note = new Note();
      }
      switchToDetail(note);
      return;
    }

    if (ConstantsBase.ACTION_SEND_AND_EXIT.equals(i.getAction())) {
      saveAndExit(i);
      return;
    }

    // Tag search
    if (Intent.ACTION_VIEW.equals(i.getAction()) && i.getDataString()
        .startsWith(UrlCompleter.HASHTAG_SCHEME)) {
      switchToList();
      return;
    }

    // Home launcher shortcut widget
    if (Intent.ACTION_VIEW.equals(i.getAction()) && i.getData() != null) {
      Long id = Long.valueOf(Uri.parse(i.getDataString()).getQueryParameter("id"));
      Note note = DbHelper.getInstance().getNote(id);
      if (note == null) {
        showMessage(R.string.note_doesnt_exist, ONStyle.ALERT);
        return;
      }
      switchToDetail(note);
      return;
    }

    // Home launcher "new note" shortcut widget
    if (ConstantsBase.ACTION_SHORTCUT_WIDGET.equals(i.getAction())) {
      switchToDetail(new Note());
      return;
    }
  }


  /**
   * Used to perform a quick text-only note saving (eg. Tasker+Pushbullet)
   */
  private void saveAndExit(Intent i) {
    Note note = new Note();
    note.setTitle(i.getStringExtra(Intent.EXTRA_SUBJECT));
    note.setContent(i.getStringExtra(Intent.EXTRA_TEXT));
    // 디비가기전
    note.setAddress(i.getStringExtra(Intent.EXTRA_UID));

    DbHelper.getInstance().updateNote(note, true);
    //showToast(getString(R.string.note_updated), Toast.LENGTH_SHORT);
    finish();
  }


  private boolean receivedIntent(Intent i) {
    return ConstantsBase.ACTION_SHORTCUT.equals(i.getAction())
        || ConstantsBase.ACTION_NOTIFICATION_CLICK.equals(i.getAction())
        || ConstantsBase.ACTION_WIDGET.equals(i.getAction())
        || ConstantsBase.ACTION_WIDGET_TAKE_PHOTO.equals(i.getAction())
        || ((Intent.ACTION_SEND.equals(i.getAction())
        || Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())
        || ConstantsBase.INTENT_GOOGLE_NOW.equals(i.getAction()))
        && i.getType() != null)
        || i.getAction().contains(ConstantsBase.ACTION_NOTIFICATION_CLICK);
  }


  private boolean noteAlreadyOpened(Note note) {
    DetailFragment detailFragment = (DetailFragment) getFragmentManagerInstance().findFragmentByTag(
        FRAGMENT_DETAIL_TAG);
    return detailFragment != null && NotesHelper.haveSameId(note, detailFragment.getCurrentNote());
  }


  public void switchToList() {
    FragmentTransaction transaction = getFragmentManagerInstance().beginTransaction();
    animateTransition(transaction, TRANSITION_HORIZONTAL);
    ListFragment mListFragment = new ListFragment();
    transaction.replace(R.id.fragment_container, mListFragment, FRAGMENT_LIST_TAG).addToBackStack
        (FRAGMENT_DETAIL_TAG).commitAllowingStateLoss();
    if (getDrawerToggle() != null) {
      getDrawerToggle().setDrawerIndicatorEnabled(false);
    }
    getFragmentManagerInstance().getFragments();
    EventBus.getDefault().post(new SwitchFragmentEvent(SwitchFragmentEvent.Direction.PARENT));
  }


  public void switchToDetail(Note note) {
    FragmentTransaction transaction = getFragmentManagerInstance().beginTransaction();
    animateTransition(transaction, TRANSITION_HORIZONTAL);
    DetailFragment mDetailFragment = new DetailFragment();
    Bundle b = new Bundle();
    b.putParcelable(ConstantsBase.INTENT_NOTE, note);
    mDetailFragment.setArguments(b);
    if (getFragmentManagerInstance().findFragmentByTag(FRAGMENT_DETAIL_TAG) == null) {
      transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG)
          .addToBackStack(FRAGMENT_LIST_TAG)
          .commitAllowingStateLoss();
    } else {
      getFragmentManagerInstance().popBackStackImmediate();
      transaction.replace(R.id.fragment_container, mDetailFragment, FRAGMENT_DETAIL_TAG)
          .addToBackStack(FRAGMENT_DETAIL_TAG)
          .commitAllowingStateLoss();
    }
  }


  /**
   * Notes sharing
   */
  public void shareNote(Note note) {

    String titleText = note.getTitle();

    String contentText = titleText
        + System.getProperty("line.separator")
        + note.getContent();

    Intent shareIntent = new Intent();
    // Prepare sharing intent with only text
    if (note.getAttachmentsList().isEmpty()) {
      shareIntent.setAction(Intent.ACTION_SEND);
      shareIntent.setType("text/plain");

      // Intent with single image attachment
    } else if (note.getAttachmentsList().size() == 1) {
      shareIntent.setAction(Intent.ACTION_SEND);
      Attachment attachment = note.getAttachmentsList().get(0);
      shareIntent.setType(attachment.getMime_type());
      shareIntent.putExtra(Intent.EXTRA_STREAM, FileProviderHelper.getShareableUri(attachment));

      // Intent with multiple images
    } else if (note.getAttachmentsList().size() > 1) {
      shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
      ArrayList<Uri> uris = new ArrayList<>();
      // A check to decide the mime type of attachments to share is done here
      HashMap<String, Boolean> mimeTypes = new HashMap<>();
      for (Attachment attachment : note.getAttachmentsList()) {
        uris.add(FileProviderHelper.getShareableUri(attachment));
        mimeTypes.put(attachment.getMime_type(), true);
      }
      // If many mime types are present a general type is assigned to intent
      if (mimeTypes.size() > 1) {
        shareIntent.setType("*/*");
      } else {
        shareIntent.setType((String) mimeTypes.keySet().toArray()[0]);
      }

      shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
    }
    shareIntent.putExtra(Intent.EXTRA_SUBJECT, titleText);
    shareIntent.putExtra(Intent.EXTRA_TEXT, contentText);

    startActivity(Intent
        .createChooser(shareIntent, getResources().getString(R.string.share_message_chooser)));
  }


  /**
   * Single note permanent deletion
   *
   * @param note Note to be deleted
   */
  public void deleteNote(Note note) {
    new NoteProcessorDelete(Collections.singletonList(note)).process();
    notifyAppWidgets(this);
    LogDelegate.d("Deleted permanently note with ID '" + note.get_id() + "'");
  }


  public void updateWidgets() {
    new UpdateWidgetsTask(getApplicationContext())
        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }


  public void showMessage(int messageId, Style style) {
    showMessage(getString(messageId), style);
  }


  public void showMessage(String message, Style style) {
    // ViewGroup used to show Crouton keeping compatibility with the new Toolbar
    runOnUiThread(
        () -> Crouton.makeText(this, message, style, binding.croutonHandle.croutonHandle).show());
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    prefsChanged = true;
  }

}
