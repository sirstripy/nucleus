package nucleus.presenter;

import android.os.Bundle;
import android.util.Printer;

import java.util.concurrent.CopyOnWriteArrayList;

import nucleus.presenter.broker.Broker;

public class Presenter<ViewType> {

    private static final String PRESENTER_ID_KEY = "id";
    private static final String PRESENTER_STATE_KEY = "state";

    public interface TargetListener<TargetType> {
        void onTakeTarget(TargetType target);
        void onDropTarget(TargetType target);
    }

    public interface OnDestroyListener {
        void onDestroy();
    }

    private static Presenter rootPresenter = new Presenter();

    private Presenter parent;
    private String id;
    private CopyOnWriteArrayList<Presenter> presenters = new CopyOnWriteArrayList<Presenter>();

    private ViewType view;

    private CopyOnWriteArrayList<TargetListener> presenterListeners = new CopyOnWriteArrayList<TargetListener>();
    private CopyOnWriteArrayList<TargetListener> viewListeners = new CopyOnWriteArrayList<TargetListener>();
    private CopyOnWriteArrayList<OnDestroyListener> onDestroyListeners = new CopyOnWriteArrayList<OnDestroyListener>();

    public static Presenter getRootPresenter() {
        return rootPresenter;
    }

    public static void setRootPresenter(Presenter rootPresenter) {
        Presenter.rootPresenter = rootPresenter;
    }

    public Presenter getParent() {
        return parent;
    }

    public String getId() {
        return id;
    }

    public ViewType getView() {
        return view;
    }

    public Presenter getPresenter(String id) {

        for (Presenter presenter : presenters) {
            if (presenter.id.equals(id))
                return presenter;
        }

        return null;
    }

    /**
     * Finds a Presenter or restores it from the saved state.
     * There can be three cases when this method is being called:
     * 1. First creation of a view;
     * 2. Restoring of a view when the process has NOT been destroyed (configuration change, activity recreation because of memory limitation);
     * 3. Restoring of a view when the process has been destroyed.
     *
     * @param creator    a callback for the Presenter class instantiation
     * @param savedState saved state of the required {@link nucleus.presenter.Presenter} that been created with
     *                   {@link nucleus.presenter.Presenter#save} or null
     * @param <T>        Type of the required presenter
     * @return found or created with {@link nucleus.presenter.PresenterCreator} presenter
     */
    public <T extends Presenter> T provide(PresenterCreator<T> creator, Bundle savedState) {

        String id = null;

        if (savedState != null) {
            id = savedState.getString(PRESENTER_ID_KEY);

            for (Presenter presenter : presenters) {
                if (presenter.id.equals(id))
                    //noinspection unchecked
                    return (T)presenter; // it should always be of the same type if the caller will not cheat
            }
        }

        T presenter = creator.createPresenter();
        ((Presenter)presenter).parent = this;
        ((Presenter)presenter).id = id != null ? id :
            presenter.getClass().getSimpleName() + " -> " + creator.getClass().getSimpleName() +
                " (" + presenters.size() + "/" + System.nanoTime() + "/" + (int)(Math.random() * Integer.MAX_VALUE) + ")";

        presenter.onCreate(savedState == null ? null : savedState.getBundle(PRESENTER_STATE_KEY));

        takePresenter(presenter);

        return presenter;
    }

    public void destroy() {
        for (Presenter presenter : presenters)
            presenter.destroy();

        parent.dropPresenter(this);

        for (OnDestroyListener listener : onDestroyListeners)
            listener.onDestroy();

        onDestroy();
    }

    public Bundle save() {
        Bundle bundle = new Bundle();
        bundle.putString(PRESENTER_ID_KEY, id);
        bundle.putBundle(PRESENTER_STATE_KEY, onSave());
        return bundle;
    }

    public void takeView(ViewType view) {
        this.view = view;

        onTakeView(view);

        for (TargetListener listener : viewListeners)
            listener.onTakeTarget(view);
    }

    public void dropView(ViewType view) {

        for (TargetListener listener : viewListeners)
            listener.onDropTarget(view);

        onDropView(view);

        this.view = null;
    }

    public void takePresenter(Presenter presenter) {
        presenters.add(presenter);

        onTakePresenter(presenter);

        for (TargetListener listener : presenterListeners)
            listener.onTakeTarget(presenter);
    }

    public void dropPresenter(Presenter presenter) {

        for (TargetListener listener : presenterListeners)
            listener.onDropTarget(presenter);

        onDropPresenter(presenter);

        presenters.remove(presenter);
    }

    protected void onCreate(Bundle savedState) {
    }

    protected void onDestroy() {
    }

    protected Bundle onSave() {
        return null;
    }

    protected void onTakeView(ViewType view) {
    }

    protected void onDropView(ViewType view) {
    }

    protected void onTakePresenter(Presenter presenter) {
    }

    protected void onDropPresenter(Presenter presenter) {
    }

    /**
     * This method attaches a {@link Broker} to a {@link Presenter}.
     * Call this during {@link Presenter#onCreate}.
     *
     * @param broker {@link Broker} to create
     * @param <T>    {@link Broker} type
     * @return The same {@link Broker} that has been passed as an argument
     */
    protected <T extends Broker<ViewType>> T addViewBroker(T broker) {
        viewListeners.add(broker);
        onDestroyListeners.add(broker);
        return broker;
    }

    protected <T extends Broker<Presenter>> T addPresenterBroker(T broker) {
        presenterListeners.add(broker);
        onDestroyListeners.add(broker);
        return broker;
    }

    protected void addViewListener(TargetListener<ViewType> listener) {
        viewListeners.add(listener);
    }

    protected void removeViewListener(TargetListener<ViewType> listener) {
        viewListeners.remove(listener);
    }

    protected void addOnDestroyListener(OnDestroyListener listener) {
        onDestroyListeners.add(listener);
    }

    protected void removeOnDestroyListener(OnDestroyListener listener) {
        onDestroyListeners.remove(listener);
    }

    // debug

    protected void print(Printer printer, int level) {
        String padding = "";
        for (int p = 0; p < level; p++)
            padding += ".   ";

        ViewType view = getView();
        printer.println(padding + (id == null ? "rootPresenter" : "id: " + id) + (view == null ? "" : " => view: " + view.toString()));

        for (Presenter m : presenters)
            m.print(printer, level + 1);
    }

    public void print(Printer printer) {
        print(printer, 0);
    }
}
